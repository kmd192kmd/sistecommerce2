package com.example.java.groupbuy.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.groupbuy.dto.GroupBuyDetailResponse;
import com.example.java.groupbuy.dto.GroupBuyDto;
import com.example.java.groupbuy.dto.GroupBuyOptionView;
import com.example.java.groupbuy.dto.GroupBuyOptionsDto;
import com.example.java.groupbuy.dto.GroupBuySummaryResponse;
import com.example.java.groupbuy.dto.ParticipateResult;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.entity.GroupBuyStatus;
import com.example.java.groupbuy.entity.Participation;
import com.example.java.groupbuy.entity.ParticipationStatus;
import com.example.java.groupbuy.entity.WaitingQueue;
import com.example.java.groupbuy.dto.ParticipateResponse;
import com.example.java.groupbuy.event.GroupBuyConfirmedEvent;
import com.example.java.groupbuy.event.GroupBuyFailedEvent;
import com.example.java.groupbuy.event.GroupBuyPaymentDoneEvent;
import com.example.java.groupbuy.event.GroupBuyPromotedEvent;
import com.example.java.groupbuy.event.GroupBuyRefundFailedEvent;
import com.example.java.groupbuy.event.GroupBuyRefundedEvent;
import com.example.java.groupbuy.payment.GroupBuyPaymentCommand;
import com.example.java.groupbuy.payment.GroupBuyPaymentPort;
import com.example.java.orders.dto.OrderCreateResultDto;
import com.example.java.orders.service.OrdersCommandService;
import com.example.java.groupbuy.repository.GroupBuyOptionsRepository;
import com.example.java.groupbuy.repository.GroupBuyRepository;
import com.example.java.groupbuy.repository.ParticipationRepository;
import com.example.java.groupbuy.repository.WaitingQueueRepository;
import com.example.java.product.entity.Product;
import com.example.java.product.entity.ProductImage;
import com.example.java.product.repository.OptionsRepository;
import com.example.java.product.repository.ProductImageRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBuyService {
	
    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionsRepository groupBuyOptionsRepository;
    private final OptionsRepository optionsRepository;
    private final ProductImageRepository productImageRepository;
    private final ParticipationRepository participationRepository;
    private final WaitingQueueRepository waitingQueueRepository;
    private final GroupBuyPaymentPort paymentPort;
    private final OrdersCommandService ordersCommandService;
    private final ApplicationEventPublisher eventPublisher;

    // ProductRepository가 dev에서 class(EntityManager 직접 구현)로 바뀌어 
    // JpaRepository 메서드가 없음.
    // 등록 시 Product FK만 세팅하면 되므로 
    // EntityManager.getReference로 프록시 참조를 얻는다.
    @PersistenceContext
    private EntityManager entityManager;

    /** 썸네일 이미지가 없을 때 사용할 기본 이미지 (static 리소스). */
    private static final String DEFAULT_IMAGE = "/src/images/product/default.png";

    /** 중복참여 판정 대상 = 진행 중인 참여 상태. 이 상태가 있으면 같은 공구 재참여를 막는다. */
    private static final List<ParticipationStatus> ACTIVE_PARTICIPATION_STATUSES =
            List.of(ParticipationStatus.PARTICIPATING, ParticipationStatus.PAYMENT_PENDING);

    /** T_lock: 마감 이 시간 전부터는 취소 불가 (현재 24h). */
    private static final long CANCEL_LOCK_HOURS = 24;
    /** T_pay: 승격자의 결제 제한시간 (현재 24h). 불변조건 T_lock >= T_pay 를 지켜야 함. */
    private static final long PROMOTION_PAY_HOURS = 24;
    /** 정규 참여자의 결제 대기시간(분). 자리 예약 후 이 시간 내 미결제면 만료 스케줄러가 반납한다. */
    private static final long REGULAR_PAY_MINUTES = 10;
    /** 공구 예정 노출 윈도우(일): 일반 회원은 시작 7일 전부터, 멤버십은 14일 전(7일 조기)부터 본다. */
    private static final long SCHEDULED_PREVIEW_DAYS_NORMAL = 7;
    private static final long SCHEDULED_PREVIEW_DAYS_MEMBER = 14;

    /**
     * 공구 등록 (관리자).
     * - 옵션별 발주수량(order_qty)의 합으로 max_count 자동 계산
     * - 상태는 SCHEDULED, occupied_count는 0으로 시작
     *
     * @param dto        공구 기본 정보 (max_count는 무시하고 옵션 합으로 계산)
     * @param optionDtos 옵션별 발주수량 목록 (최소 1개)
     * @return 생성된 공구 seq
     * 
     * @Transactional: 이 어노테이션이 붙은 메서드 안의 DB 작업을
     * 전부 한 묶음으로 처리함
     * 공구 1건 저장하고, 옵션 3개 저장하는 도중에 에러가 나면,
     * 이미 저장한 공구 1건도 전부 취소됨
     */
    
    // 공동 구매 생성 메서드
    @Transactional
    public Long create(GroupBuyDto dto, List<GroupBuyOptionsDto> optionDtos) {
        if (optionDtos == null || optionDtos.isEmpty()) {
            throw new IllegalArgumentException("옵션은 최소 1개 이상이어야 합니다.");
        }

        // 옵션별 발주수량 검증 + max_count 자동 계산 (max_count = 옵션 order_qty의 합)
        int maxCount = 0;
        for (GroupBuyOptionsDto od : optionDtos) {
            Integer orderQty = od.getOrderQty();
            if (orderQty == null || orderQty < 1) {
                throw new IllegalArgumentException("옵션 발주수량은 1 이상이어야 합니다.");
            }
            maxCount += orderQty;
        }

        // 최소 인원 불변조건: 1 <= min_count <= max_count
        Integer minCount = dto.getMinCount();
        if (minCount == null || minCount < 1) {
            throw new IllegalArgumentException("최소 인원은 1 이상이어야 합니다.");
        }
        if (minCount > maxCount) {
            throw new IllegalArgumentException(
                    "최소 인원(" + minCount + ")이 최대 인원(" + maxCount + ")보다 클 수 없습니다.");
        }

        // 진행 기간 검증
        if (dto.getStartAt() == null || dto.getEndAt() == null
                || !dto.getStartAt().isBefore(dto.getEndAt())) {
            throw new IllegalArgumentException("공구 시작 시각은 종료 시각보다 앞서야 합니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        GroupBuy groupBuy = GroupBuy.builder()
                .product(entityManager.getReference(Product.class, dto.getProductSeq()))
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .createdAt(now)
                .minCount(minCount)
                .maxCount(maxCount)
                .originalPrice(dto.getOriginalPrice())
                .finalPrice(dto.getFinalPrice())
                .status(GroupBuyStatus.SCHEDULED)
                .build();
        GroupBuy saved = groupBuyRepository.save(groupBuy);

        for (GroupBuyOptionsDto od : optionDtos) {
            GroupBuyOptions option = GroupBuyOptions.builder()
                    .groupBuy(saved)
                    .options(optionsRepository.getReferenceById(od.getOptionsSeq()))
                    .orderQty(od.getOrderQty())
                    .occupiedCount(0)
                    .build();
            groupBuyOptionsRepository.save(option);
        }

        return saved.getSeq();
    }

    /**
     * 공구 참여 신청 (2단계: 정규 참여 + 대기열 등록 분기).
     *
     * 선택한 옵션의 매진 여부에 따라 결과가 갈린다:
     *  - 정원이 남으면 → 정규 참여(점유 +1, 결제, participation INSERT) → {@link ParticipateResult#PARTICIPATED}
     *  - 매진이면     → 대기열(waiting_queue)에 등록(결제·점유 없음)      → {@link ParticipateResult#QUEUED}
     *
     * 흐름:
     *  1) findBySeqForUpdate 로 옵션 행에 비관적 쓰기 락(SELECT ... FOR UPDATE) → 같은 옵션 경쟁을 줄세움
     *  2) 옵션-공구 일치 + 공구 진행상태(ONGOING)/기간 검증
     *  3) 중복 검사(공구 단위): 이미 활성 참여 중이거나, 이미 대기열에 있으면 거부
     *  4) 매진 여부로 분기
     *     - 매진     → waiting_queue INSERT 후 QUEUED 반환 (점유·결제 안 함)
     *     - 정원 남음 → occupy(+1) → 결제(스텁) → participation INSERT 후 PARTICIPATED 반환
     *
     * 매진 판단(option.isSoldOut())을 옵션 행 락 안에서 하는 이유:
     * occupied_count 를 읽어 매진을 판정하는데, 락 없이 읽으면 동시 참여가 같은 자리를
     * 둘 다 "정원 남음"으로 보고 함께 occupy 해 정원을 초과할 수 있다.
     * 락이 commit까지 유지되므로 동시 참여가 직렬화되어 orderQty(옵션 정원) 초과가 차단된다 (NFR-001).
     *
     * @param groupBuySeq 참여할 공구 seq
     * @param optionSeq   선택한 group_buy_options.seq
     * @param memberSeq   참여 회원 seq
     * @return 정규 참여 성공이면 PARTICIPATED, 매진으로 대기열 등록되면 QUEUED
     */
    @Transactional
    public ParticipateResponse participate(Long groupBuySeq, Long optionSeq, Long memberSeq) {
        // 1) 옵션 행 비관적 락 (동시 참여 직렬화)
        GroupBuyOptions option = groupBuyOptionsRepository.findBySeqForUpdate(optionSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 공구 옵션을 찾을 수 없습니다. seq=" + optionSeq));

        // 2) 방어 검증: 옵션-공구 일치 + 공구 진행상태/기간
        GroupBuy groupBuy = option.getGroupBuy();
        if (!groupBuy.getSeq().equals(groupBuySeq)) {
            throw new IllegalArgumentException("옵션이 해당 공동구매에 속하지 않습니다. groupBuySeq=" + groupBuySeq);
        }
        if (groupBuy.getStatus() != GroupBuyStatus.ONGOING) {
            throw new IllegalStateException("진행 중인 공동구매가 아닙니다.");
        }
        if (!LocalDateTime.now().isBefore(groupBuy.getEndAt())) {
            throw new IllegalStateException("이미 마감된 공동구매입니다.");
        }

        // 3) 중복 검사 (공구 단위, 1인 1상품 원칙). 매진 여부와 무관하게 먼저 막는다.
        //    3-1) 이미 활성 참여 중(PARTICIPATING/PAYMENT_PENDING)이면 거부 (취소 후 재참여는 허용)
        if (participationRepository.existsByGroupBuySeqAndMemberSeqAndStatusIn(
                groupBuySeq, memberSeq, ACTIVE_PARTICIPATION_STATUSES)) {
            throw new IllegalStateException("이미 이 공동구매에 참여 중입니다.");
        }
        //    3-2) 이미 이 공구 대기열에 등록돼 있으면 거부 (다른 옵션 대기열이라도 막음)
        if (waitingQueueRepository.existsByGroupBuySeqAndMemberSeq(groupBuySeq, memberSeq)) {
            throw new IllegalStateException("이미 이 공동구매 대기열에 등록되어 있습니다.");
        }

        // 4) 매진 여부로 분기 (판단은 위 옵션 행 락 안에서 이뤄진다)
        if (option.isSoldOut()) {
            // 4-A) 매진 → 대기열 등록. 점유(occupied_count)·결제는 건드리지 않는다.
            //      대기열 대기자는 아직 점유 인원이 아니며(occupied = PARTICIPATING + PAYMENT_PENDING),
            //      앞사람이 이탈해 자리가 나면 FIFO(created_at)로 승격된다.
            waitingQueueRepository.save(WaitingQueue.builder()
                    .groupBuy(groupBuy)
                    .groupBuyOptions(option)
                    .memberSeq(memberSeq)
                    .createdAt(LocalDateTime.now())   // FIFO 승격 순서의 기준이 되는 등록 시각
                    .build());
            return ParticipateResponse.queued();
        }

        // 4-B) 정원 남음 → 자리 예약(결제 전). 실제 결제는 토스 결제창에서 별도로 진행한다(2단계).
        //      오버셀 방지를 위해 결제를 기다리지 않고 지금 점유한다(occupy). 미결제로 이탈하면
        //      만료 스케줄러가 결제기한 경과 시 자리를 반납(release)하고 대기열을 승격시킨다.
        option.occupy(); // occupy()는 내부에서도 매진을 재검사해 이중 방어(NFR-001)

        // 결제 대기(PAYMENT_PENDING)로 참여 INSERT. 결제기한 = min(now + 10분, 마감) — 미결제 자리를 오래 묶지 않는다.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(REGULAR_PAY_MINUTES);
        if (deadline.isAfter(groupBuy.getEndAt())) {
            deadline = groupBuy.getEndAt();
        }
        Participation participation = participationRepository.save(Participation.builder()
                .groupBuy(groupBuy)
                .groupBuyOptions(option)
                .memberSeq(memberSeq)
                .status(ParticipationStatus.PAYMENT_PENDING)
                .paymentDeadline(deadline)
                .createdAt(now)
                .build());

        // 결제 대기 주문 생성(orders + order_item, participation_seq 채움) → orderUid 반환.
        // 가격 스냅샷 계산은 buildPaymentCommand 재사용. 실제 결제는 프론트가 이 orderUid로 토스 결제창에서 한다.
        GroupBuyPaymentCommand snapshot = buildPaymentCommand(groupBuy, option, participation.getSeq(), memberSeq);
        OrderCreateResultDto order = ordersCommandService.createGroupBuyOrder(
                snapshot.memberSeq(), snapshot.participationSeq(), snapshot.optionsSeq(),
                snapshot.originalPrice(), snapshot.finalPrice(), snapshot.participationDiscount());

        return ParticipateResponse.paymentRequired(order);
    }

    /**
     * 공구 참여 취소 (정규 참여자) + 점유 복구 + 같은 옵션 대기열 FIFO 승격.
     *
     * 한 트랜잭션으로 "취소 → 환불 → 점유 복구 → 승격"을 묶는다 (NFR-005 신뢰성).
     *
     * 흐름:
     *  1) 취소 대상(PARTICIPATING) 참여 조회 → 없으면 거부. 옵션 seq도 여기서 얻는다.
     *  2) 옵션 행 비관적 락 → 같은 옵션의 참여/취소/승격을 직렬화 (NFR-001)
     *  3) 락 획득 후 참여 상태 재확인(refresh) → 이미 CANCELLED면 환불 없이 종료 (멱등성, NFR-004)
     *  4) T_lock 검증: 마감 24h 전부터는 취소 불가
     *  5) 참여 CANCELLED + 환불
     *  6) 점유 복구(release): 이 자리를 비운다
     *  7) 같은 옵션 대기열 FIFO 1명 승격 (있으면): occupy + 대기열 행 삭제 + PAYMENT_PENDING participation 생성
     *
     * @param groupBuySeq 취소할 공구 seq
     * @param memberSeq   취소 요청 회원 seq
     */
    @Transactional
    public void cancel(Long groupBuySeq, Long memberSeq) {
        // 1) 취소 대상(정규 참여) 조회. 옵션 seq를 얻어 다음 단계에서 그 행을 잠근다.
        Participation participation = participationRepository
                .findFirstByGroupBuySeqAndMemberSeqAndStatus(
                        groupBuySeq, memberSeq, ParticipationStatus.PARTICIPATING)
                .orElseThrow(() -> new IllegalStateException("취소할 참여가 없습니다."));

        // 2) 옵션 행 비관적 락 (참여 때와 같은 행을 잠가 점유 증감 경쟁을 직렬화)
        GroupBuyOptions option = groupBuyOptionsRepository
                .findBySeqForUpdate(participation.getGroupBuyOptions().getSeq())
                .orElseThrow(() -> new IllegalStateException("옵션을 찾을 수 없습니다."));

        // 3) 멱등성(NFR-004): 동시에 들어온 중복 취소 중 
        //    먼저 온 요청이 락 안에서 CANCELLED로 바꾸고 commit한다.
        //    락을 이어받은 두 번째 요청은 여기서 최신 상태를 다시 읽어, 
        //    이미 취소됐으면 환불 없이 종료한다.
        entityManager.refresh(participation);
        if (participation.getStatus() != ParticipationStatus.PARTICIPATING) {
            return; // 이미 취소 처리됨 → 환불 중복 방지
        }

        GroupBuy groupBuy = option.getGroupBuy();
        LocalDateTime now = LocalDateTime.now();

        // 4) T_lock 검증: 마감 CANCEL_LOCK_HOURS(24h) 전부터는 취소 불가.
        //    이 구간 이후 취소를 허용하면 
        //    승격자 결제기한(min(now+24h, 마감))이 사실상 마감에 몰려
        //    마감 정합성이 흔들린다 → 불변조건 T_lock >= T_pay 를 시간 정책으로 보장.
        if (!now.isBefore(groupBuy.getEndAt().minusHours(CANCEL_LOCK_HOURS))) {
            throw new IllegalStateException("안내: 마감 " + CANCEL_LOCK_HOURS + "시간 이내에는 취소할 수 없습니다.");
        }

        // 5) 취소 확정 + 환불 (위 상태검사로 환불은 1회만 보장됨)
        participation.cancel(); // status → CANCELLED (변경감지로 UPDATE)
        // 환불 = participationSeq로 원주문(order_item)을 찾아 PG 취소. 금액은 order_item.final_price 스냅샷 사용(역산 X).
        paymentPort.refund(participation.getSeq());
        // 환불완료 알림은 AFTER_COMMIT 리스너가 처리(이 트랜잭션이 커밋돼야 발송).
        eventPublisher.publishEvent(
                new GroupBuyRefundedEvent(participation.getMemberSeq(), participation.getSeq()));

        // 6) 점유 복구: 이 자리를 비운다 (occupied_count -1)
        option.release();

        // 7) 같은 옵션 대기열 FIFO 1명 자동 승격 (있으면). 취소·만료가 공유하는 로직.
        promoteNextWaiting(option, groupBuy, now);
    }

    /**
     * 같은 옵션 대기열의 FIFO(created_at 최소) 1명을 결제대기(PAYMENT_PENDING)로 승격시킨다 (대기자가 있으면).
     * 취소(cancel)·결제기한 만료(expirePromotion)에서 공통으로 호출한다 (NFR-002 공정성).
     *
     * 전제: 호출 전에 release()로 자리가 1개 비어 있어야 occupy()가 성공한다
     *       (release -1 → 승격 occupy +1 = 순증감 0: 빈자리를 승격자가 그대로 이어받음).
     *
     * 승격자는 아직 결제 전이라 PAYMENT_PENDING으로만 INSERT하고 orders/payment는 만들지 않는다
     * (승격자가 실제 결제할 때 한 트랜잭션으로 생성). 결제기한은 min(now + T_pay, 마감)으로 잘라
     * 마감을 절대 넘기지 않게 한다(불변조건 T_lock >= T_pay).
     */
    private void promoteNextWaiting(GroupBuyOptions option, GroupBuy groupBuy, LocalDateTime now) {
        waitingQueueRepository.findFirstByGroupBuyOptionsOrderByCreatedAtAsc(option)
                .ifPresent(next -> {
                    option.occupy();                     // 빈자리 점유
                    waitingQueueRepository.delete(next); // 대기 이탈 = 행 삭제

                    LocalDateTime deadline = now.plusHours(PROMOTION_PAY_HOURS);
                    if (deadline.isAfter(groupBuy.getEndAt())) {
                        deadline = groupBuy.getEndAt();
                    }

                    Participation promoted = participationRepository.save(Participation.builder()
                            .groupBuy(groupBuy)
                            .groupBuyOptions(option)
                            .memberSeq(next.getMemberSeq())
                            .status(ParticipationStatus.PAYMENT_PENDING)
                            .paymentDeadline(deadline)  // 승격자 결제 제한시각
                            .promotedAt(now)            // 승격된 시각
                            .createdAt(now)
                            .build());

                    // 승격 알림(기한 내 결제 안내)은 AFTER_COMMIT 리스너가 처리.
                    eventPublisher.publishEvent(
                            new GroupBuyPromotedEvent(promoted.getMemberSeq(), promoted.getSeq()));
                });
    }

    /**
     * 승격자 결제 시작 (대기열에서 승격된 결제대기자가 기한 내 직접 결제).
     *
     * 승격 시 이미 옵션을 점유(occupied_count +1)했으므로 점유는 건드리지 않는다.
     * 이 메서드는 그 참여에 대한 '결제 대기' 주문을 만들어 orderUid를 돌려줄 뿐이고,
     * 실제 결제는 프론트가 토스 결제창에서 진행한다. 결제 성공 시 confirmAfterPayment가
     * PAYMENT_PENDING → PARTICIPATING으로 확정한다 (정규 참여와 동일한 2단계 흐름으로 합류).
     *
     * 흐름:
     *  1) 결제대기(PAYMENT_PENDING) 참여 조회 → 없으면 거부
     *  2) 옵션 행 락 → 만료 처리(스케줄러)·취소와 직렬화
     *  3) refresh로 상태 재확인 → 그 사이 만료되거나 이미 결제됐으면 거부
     *  4) 공구 ONGOING + 결제기한(now ≤ payment_deadline) 검증
     *  5) 결제 대기 주문 생성 → orderUid 반환 (결제 자체는 토스 결제창에서)
     *
     * 	@param groupBuySeq 결제할 공구 seq
     * 	@param memberSeq   승격된 회원 seq
     * 	@return 결제 대기 주문 정보(orderUid/amount) — 프론트가 토스 결제창에 사용
     */
    @Transactional
    public ParticipateResponse startPromotedPayment(Long groupBuySeq, Long memberSeq) {
        // 1) 결제대기(승격) 참여 조회. 옵션 seq를 얻어 다음 단계에서 그 행을 잠근다.
        Participation participation = participationRepository
                .findFirstByGroupBuySeqAndMemberSeqAndStatus(
                        groupBuySeq, memberSeq, ParticipationStatus.PAYMENT_PENDING)
                .orElseThrow(() -> new IllegalStateException("결제 대기 중인 참여가 없습니다."));

        // 2) 옵션 행 비관적 락 (만료 처리/취소가 같은 행을 건드리므로 직렬화)
        GroupBuyOptions option = groupBuyOptionsRepository
                .findBySeqForUpdate(participation.getGroupBuyOptions().getSeq())
                .orElseThrow(() -> new IllegalStateException("옵션을 찾을 수 없습니다."));

        // 3) 락 획득 후 상태 재확인: 락 대기 중에 만료(FAILED) 처리되거나 이미 결제됐을 수 있다.
        //    PAYMENT_PENDING이 아니면 더 진행하지 않는다 (중복 결제·만료자 결제 방지).
        entityManager.refresh(participation);
        if (participation.getStatus() != ParticipationStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("이미 처리되었거나 결제 기한이 만료된 참여입니다.");
        }

        GroupBuy groupBuy = option.getGroupBuy();
        LocalDateTime now = LocalDateTime.now();

        // 4) 공구 진행 상태 + 결제기한 검증 (만료 자동처리(GB-023) 전이라 여기서도 직접 방어)
        if (groupBuy.getStatus() != GroupBuyStatus.ONGOING) {
            throw new IllegalStateException("진행 중인 공동구매가 아닙니다.");
        }
        if (participation.getPaymentDeadline() != null && now.isAfter(participation.getPaymentDeadline())) {
            throw new IllegalStateException("결제 기한이 지났습니다.");
        }

        // 5) 결제 대기 주문 생성(orders + order_item, participation_seq 채움) → orderUid 반환.
        //    승격자는 participation이 이미 존재하므로 그 seq로 주문을 만든다. 가격 스냅샷은 buildPaymentCommand 재사용.
        //    실제 결제는 프론트가 이 orderUid로 토스 결제창에서 진행하고, 성공 시 confirmAfterPayment가 확정한다.
        GroupBuyPaymentCommand snapshot = buildPaymentCommand(groupBuy, option, participation.getSeq(), memberSeq);
        OrderCreateResultDto order = ordersCommandService.createGroupBuyOrder(
                snapshot.memberSeq(), snapshot.participationSeq(), snapshot.optionsSeq(),
                snapshot.originalPrice(), snapshot.finalPrice(), snapshot.participationDiscount());

        return ParticipateResponse.paymentRequired(order);
    }

    /**
     * 승격자 결제기한 만료 처리 (스케줄러가 만료 대상마다 1건씩 호출).
     *
     * 결제대기(PAYMENT_PENDING) 상태로 결제기한을 넘긴 승격자를 EXPIRED로 만들고,
     * 점유를 복구(release)한 뒤 같은 옵션의 다음 대기자를 승격시킨다.
     * 승격자는 결제 전이라 환불은 없다.
     *
     * 흐름:
     *  1) participation 조회 (스케줄러가 넘긴 seq)
     *  2) 옵션 행 락 → 결제(confirmPromotedPayment)·취소와 직렬화
     *  3) refresh 상태 재확인 → 그 사이 결제 완료(PARTICIPATING)됐으면 만료 안 함 (경쟁 방어)
     *  4) 기한 재확인 (조회 시점 이후 변화 방어)
     *  5) EXPIRED 전이 + 점유 복구(release) + 다음 대기자 승격
     *
     * @param participationSeq 만료시킬 결제대기 참여 seq
     */
    @Transactional
    public void expirePromotion(Long participationSeq) {
        Participation participation = participationRepository.findById(participationSeq).orElse(null);
        if (participation == null) {
            return; // 이미 사라진 경우 — 무시
        }

        // 2) 옵션 행 비관적 락 (결제/취소와 같은 행 경쟁을 직렬화)
        GroupBuyOptions option = groupBuyOptionsRepository
                .findBySeqForUpdate(participation.getGroupBuyOptions().getSeq())
                .orElseThrow(() -> new IllegalStateException("옵션을 찾을 수 없습니다."));

        // 3) 락 획득 후 상태 재확인: 락 대기 중 승격자가 결제를 끝냈을 수 있다.
        //    PAYMENT_PENDING이 아니면 만료시키지 않는다 (결제 완료자를 만료시키는 사고 방지).
        entityManager.refresh(participation);
        if (participation.getStatus() != ParticipationStatus.PAYMENT_PENDING) {
            return;
        }

        // 4) 기한 재확인 (스케줄러 조회 후 시점까지의 안전장치)
        LocalDateTime now = LocalDateTime.now();
        if (participation.getPaymentDeadline() == null || !now.isAfter(participation.getPaymentDeadline())) {
            return; // 아직 기한 안 지남 → 만료 보류
        }

        GroupBuy groupBuy = option.getGroupBuy();

        // 5) 만료 확정 + 점유 복구 + 다음 대기자 승격 (환불 없음 — 승격자는 결제 전)
        participation.expire();          // status → EXPIRED
        option.release();                // occupied_count -1 (자리 반납)
        promoteNextWaiting(option, groupBuy, now); // 같은 옵션 FIFO 다음 1명 승격
    }

    /**
     * 결제대기자의 명시적 결제 취소 처리 (토스 결제창에서 '취소'를 눌러 failUrl로 돌아온 경우).
     *
     * 결제대기(PAYMENT_PENDING) 참여를 CANCELLED로 돌리고 점유를 복구(release)한 뒤
     * 같은 옵션의 다음 대기자를 승격시킨다. 결제 전이라 환불은 없다.
     *
     * 만료(expirePromotion)와 동일한 자리 반납 로직이며, 차이는 두 가지다:
     *  - 상태를 EXPIRED가 아니라 CANCELLED로 둔다 (시간 만료가 아니라 사용자의 능동 취소).
     *  - 결제기한 검증을 하지 않는다 — 사용자가 직접 취소했으므로 기한과 무관하게 즉시 반납한다.
     *
     * 멱등: 이미 처리됐거나(만료 EXPIRED / 결제완료 PARTICIPATING) 사라진 참여면 아무 것도 하지 않는다
     * (중복 failUrl 콜백·경쟁 방어).
     *
     * @param participationSeq 취소할 결제대기 참여 seq
     */
    @Transactional
    public void cancelPendingPayment(Long participationSeq) {
        Participation participation = participationRepository.findById(participationSeq).orElse(null);
        if (participation == null) {
            return; // 이미 사라진 경우 — 무시
        }

        // 옵션 행 비관적 락 (결제/만료/취소가 같은 행을 건드리므로 직렬화)
        GroupBuyOptions option = groupBuyOptionsRepository
                .findBySeqForUpdate(participation.getGroupBuyOptions().getSeq())
                .orElseThrow(() -> new IllegalStateException("옵션을 찾을 수 없습니다."));

        // 락 획득 후 상태 재확인: 락 대기 중 결제가 완료(PARTICIPATING)되거나 만료(EXPIRED)됐을 수 있다.
        // PAYMENT_PENDING이 아니면 취소하지 않는다 (멱등 — 중복 콜백/경쟁 방어).
        entityManager.refresh(participation);
        if (participation.getStatus() != ParticipationStatus.PAYMENT_PENDING) {
            return;
        }

        GroupBuy groupBuy = option.getGroupBuy();
        LocalDateTime now = LocalDateTime.now();

        // 취소 확정 + 점유 복구 + 다음 대기자 승격 (환불 없음 — 결제 전)
        participation.cancel();          // status → CANCELLED
        option.release();                // occupied_count -1 (자리 반납)
        promoteNextWaiting(option, groupBuy, now); // 같은 옵션 FIFO 다음 1명 승격
    }

    /**
     * 결제대기자의 명시적 취소 (공구 + 로그인 회원 기준). 화면에서 호출하는 진입점.
     *
     * 토스 결제창에서 사용자가 '취소'를 누르면 failUrl이 아니라 requestPayment Promise가 reject되므로
     * (모달이라 URL 리다이렉트 없음), 프론트의 catch에서 이 API를 호출해 자리를 즉시 반납한다.
     * 1인 1상품이라 공구 + 회원이면 결제대기 참여가 유일하게 특정된다.
     *
     * 멱등: 결제대기 참여가 없으면(이미 결제완료/만료/취소) 아무 것도 하지 않는다.
     */
    @Transactional
    public void cancelPendingPaymentByMember(Long groupBuySeq, Long memberSeq) {
        participationRepository
                .findFirstByGroupBuySeqAndMemberSeqAndStatus(
                        groupBuySeq, memberSeq, ParticipationStatus.PAYMENT_PENDING)
                .ifPresent(p -> cancelPendingPayment(p.getSeq()));
    }

    /**
     * 공구 마감 처리 (스케줄러가 마감 시각 지난 공구마다 1건씩 호출) — 확정/무산 판정.
     *
     * 확정 인원은 결제 완료(PARTICIPATING) 수만 센다. 결제대기(PAYMENT_PENDING) 승격자는
     * 아직 돈을 안 냈으므로 확정 인원에서 제외한다 — 그래야 "확정 인원 = 결제 완료 건수"가
     * 성립한다 (NFR-003 정합성).
     *
     * 흐름:
     *  1) 공구 조회 + ONGOING 확인 (아니면 skip → 이미 마감됐거나 중단된 공구 재처리 방지)
     *  2) 마감 시각 도래 확인 (스케줄러가 거르지만 방어)
     *  3) 남은 PAYMENT_PENDING → EXPIRED (결제 못하고 마감, 환불 없음)
     *  4) 판정:
     *     - 확정(결제완료 ≥ min_count): 공구 CONFIRMED + 참여자 전원 CONFIRMED
     *     - 무산(미달): 공구 FAILED + 참여자 전원 환불 후 FAILED
     *
     * @param groupBuySeq 마감 처리할 공구 seq
     */
    @Transactional
    public void close(Long groupBuySeq) {
        // 1) ONGOING 공구만 처리. status를 CONFIRMED/FAILED로 바꾸므로 다음 틱에는 자동으로 걸러져
        //    재진입(중복 마감·중복 환불)이 방지된다.
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuySeq).orElse(null);
        if (groupBuy == null || groupBuy.getStatus() != GroupBuyStatus.ONGOING) {
            return;
        }

        // 2) 마감 시각 도래 방어
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(groupBuy.getEndAt())) {
            return;
        }

        // 3) 마감 순간까지 결제 못한 승격자(PAYMENT_PENDING) → EXPIRED (환불 없음 — 결제 전)
        participationRepository.findByGroupBuySeqAndStatus(groupBuySeq, ParticipationStatus.PAYMENT_PENDING)
                .forEach(Participation::expire);

        // 4) 확정 인원 = 결제 완료자(PARTICIPATING). 이들이 곧 확정/무산 처리 대상이다.
        List<Participation> participating = participationRepository
                .findByGroupBuySeqAndStatus(groupBuySeq, ParticipationStatus.PARTICIPATING);

        if (participating.size() >= groupBuy.getMinCount()) {
            // 확정: 공구 + 결제 완료자 전원 CONFIRMED (환불 없음 — 성사됐으니 결제 유지)
            groupBuy.confirm(now);
            participating.forEach(p -> {
                p.confirm();
                // 확정 알림은 AFTER_COMMIT 리스너가 처리.
                eventPublisher.publishEvent(new GroupBuyConfirmedEvent(p.getMemberSeq(), p.getSeq()));
            });
        } else {
            // 무산: 공구 FAILED + 결제 완료자 전원 환불 후 FAILED (전원 일괄 결제취소)
            groupBuy.fail(now);
            participating.forEach(p -> {
                // 한 명의 PG 환불 실패가 나머지 전원의 마감을 막지 않도록 격리한다 (NFR-006).
                // 실패 건은 로그로 남기고 FAILED 처리는 진행 — 환불 재시도는 별도 처리(미구현).
                try {
                    paymentPort.refund(p.getSeq()); // 환불액은 order_item.final_price 스냅샷 사용(역산 X)
                    eventPublisher.publishEvent(new GroupBuyRefundedEvent(p.getMemberSeq(), p.getSeq()));
                } catch (Exception e) {
                    log.error("[공구 무산 환불] 환불 실패 participationSeq={} (마감은 계속 진행)", p.getSeq(), e);
                    eventPublisher.publishEvent(new GroupBuyRefundFailedEvent(p.getMemberSeq(), p.getSeq()));
                }
                p.fail();
                // 무산 알림은 AFTER_COMMIT 리스너가 처리.
                eventPublisher.publishEvent(new GroupBuyFailedEvent(p.getMemberSeq(), p.getSeq()));
            });
        }
    }

    /**
     * 공구 시작 처리 (스케줄러가 시작 시각 지난 예정 공구마다 호출). SCHEDULED → ONGOING.
     *
     * @param groupBuySeq 시작할 공구 seq
     */
    @Transactional
    public void open(Long groupBuySeq) {
        GroupBuy groupBuy = groupBuyRepository.findById(groupBuySeq).orElse(null);
        // SCHEDULED 공구만 시작. status 변경으로 다음 틱에는 자동으로 걸러져 중복 처리가 방지된다.
        if (groupBuy == null || groupBuy.getStatus() != GroupBuyStatus.SCHEDULED) {
            return;
        }
        // 시작 시각 도래 방어 (스케줄러가 거르지만 직접 호출 대비)
        if (LocalDateTime.now().isBefore(groupBuy.getStartAt())) {
            return;
        }
        groupBuy.open(); // SCHEDULED → ONGOING (변경감지로 UPDATE)
    }

    /**
     * 옵션별 실제 공구 결제가 = 공구 기준 할인가(final_price) + 그 옵션의 추가금(options.additional_price).
     * 팀 합의: 제일 싼 옵션을 기준가로 두고 additional_price(0 이상)로 옵션별 가격차를 표현한다.
     * 결제·환불에서 공통으로 쓴다(참여/승격결제/취소/무산환불 모두 이 가격 기준).
     */
    /**
     * 결제 완료 후 공구 참여 확정 (결제 성공 이벤트를 받은 리스너가 호출).
     *
     * 참여 신청 때 옵션을 이미 점유(occupied_count +1)하고 participation은 PAYMENT_PENDING이었다.
     * 결제가 끝났으므로 PARTICIPATING으로 확정한다 — 점유 수는 변하지 않는다(결제=확정, NFR-003).
     * 옵션 행을 잠가 취소/만료 스케줄러와 직렬화하고, 상태 재확인으로 멱등을 보장한다
     * (만료(EXPIRED)·중복 콜백 등으로 이미 PAYMENT_PENDING이 아니면 아무것도 안 함).
     */
    @Transactional
    public void confirmAfterPayment(Long participationSeq) {
        Participation participation = participationRepository.findById(participationSeq)
                .orElseThrow(() -> new IllegalStateException("참여를 찾을 수 없습니다. seq=" + participationSeq));

        // 옵션 행 락 — 취소/만료와 같은 행을 건드리므로 직렬화 (반환값은 락 목적이라 쓰지 않음)
        groupBuyOptionsRepository.findBySeqForUpdate(participation.getGroupBuyOptions().getSeq())
                .orElseThrow(() -> new IllegalStateException("옵션을 찾을 수 없습니다."));

        entityManager.refresh(participation);
        if (participation.getStatus() != ParticipationStatus.PAYMENT_PENDING) {
            return; // 이미 확정/취소/만료됨 — 멱등(중복 결제 콜백 방어)
        }
        participation.confirmPayment(); // PAYMENT_PENDING → PARTICIPATING (변경감지로 UPDATE)
        // 결제완료 알림은 AFTER_COMMIT 리스너가 처리.
        eventPublisher.publishEvent(
                new GroupBuyPaymentDoneEvent(participation.getMemberSeq(), participation.getSeq()));
    }

    private int optionFinalPrice(GroupBuy groupBuy, GroupBuyOptions option) {
        Integer additional = option.getOptions() != null ? option.getOptions().getAdditionalPrice() : null;
        return groupBuy.getFinalPrice() + (additional != null ? additional : 0);
    }

    /**
     * 공구 참여/승격 결제 요청(GroupBuyPaymentCommand)을 만든다. 가격은 모두 구매 시점 스냅샷이다.
     * final = 공구 할인가 + 옵션 추가금, original = 공구 정가 + 옵션 추가금,
     * 할인액 = original - final (= 공구 정가 - 공구 할인가, 옵션 무관 일정).
     */
    private GroupBuyPaymentCommand buildPaymentCommand(GroupBuy groupBuy, GroupBuyOptions option,
                                                       Long participationSeq, Long memberSeq) {
        int finalPrice = optionFinalPrice(groupBuy, option);          // 할인가 + 옵션 추가금
        int additional = finalPrice - groupBuy.getFinalPrice();       // 옵션 추가금
        int originalPrice = groupBuy.getOriginalPrice() + additional; // 정가 + 옵션 추가금
        return new GroupBuyPaymentCommand(
                memberSeq,
                participationSeq,
                option.getOptions() != null ? option.getOptions().getSeq() : null,
                originalPrice,
                finalPrice,
                originalPrice - finalPrice
        );
    }

    /** 공구 목록 조회. */
    @Transactional(readOnly = true)
    public List<GroupBuyDto> findAll() {
        return groupBuyRepository.findAll().stream()
                .map(GroupBuyDto::toDto)
                .toList();
    }

    /** 공구 단건 조회. */
    @Transactional(readOnly = true)
    public GroupBuyDto findById(Long seq) {
        GroupBuy groupBuy = groupBuyRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("공구를 찾을 수 없습니다. seq=" + seq));
        return GroupBuyDto.toDto(groupBuy);
    }

    /** 특정 공구의 옵션 목록 조회 (내부용). */
    @Transactional(readOnly = true)
    public List<GroupBuyOptionsDto> findOptions(Long groupBuySeq) {
        return groupBuyOptionsRepository.findByGroupBuySeq(groupBuySeq).stream()
                .map(GroupBuyOptionsDto::toDto)
                .toList();
    }

    // ===================== 회원 노출용 REST 응답 =====================

    /** 공구 목록 응답 (회원용). */
    @Transactional(readOnly = true)
    public List<GroupBuySummaryResponse> getSummaries() {
        LocalDateTime now = LocalDateTime.now();
        return groupBuyRepository.findByStatusOrderByEndAtAsc(GroupBuyStatus.ONGOING).stream()
                .map(g -> toSummary(g, now, false))
                .toList();
    }

    /**
     * 공구 예정 목록 응답 (회원용) — SCHEDULED 중 노출 윈도우 내, 시작 임박순. 남은시간은 '시작까지' 기준.
     * 멤버십(활성 구독) 회원은 시작 14일 전부터, 일반/비로그인은 7일 전부터 본다(멤버십 7일 조기 열람).
     */
    @Transactional(readOnly = true)
    public List<GroupBuySummaryResponse> getScheduledSummaries(boolean isMember) {
        LocalDateTime now = LocalDateTime.now();
        long previewDays = isMember ? SCHEDULED_PREVIEW_DAYS_MEMBER : SCHEDULED_PREVIEW_DAYS_NORMAL;
        LocalDateTime cutoff = now.plusDays(previewDays);
        return groupBuyRepository
                .findByStatusAndStartAtBeforeOrderByStartAtAsc(GroupBuyStatus.SCHEDULED, cutoff).stream()
                .map(g -> toSummary(g, now, true))
                .toList();
    }

    /** 공구 상세 응답 (회원용). 옵션은 매진 여부만 노출. */
    @Transactional(readOnly = true)
    public GroupBuyDetailResponse getDetail(Long seq) {
        GroupBuy g = groupBuyRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("공구를 찾을 수 없습니다. seq=" + seq));

        List<GroupBuyOptionView> options = groupBuyOptionsRepository.findByGroupBuySeq(seq).stream()
                .map(opt -> GroupBuyOptionView.from(opt, g.getFinalPrice()))
                .toList();

        long remain = remainSeconds(g.getEndAt(), LocalDateTime.now());
        int currentCount = countParticipating(seq);

        return GroupBuyDetailResponse.builder()
                .seq(g.getSeq())
                .status(g.getStatus())
                .productName(g.getProduct().getProductName())
                .image(thumbnailUrl(g.getProduct().getSeq()))
                .description(g.getProduct().getContent())
                .originalPrice(g.getOriginalPrice())
                .finalPrice(g.getFinalPrice())
                .discountRate(discountRate(g.getOriginalPrice(), g.getFinalPrice()))
                .startAt(g.getStartAt())
                .endAt(g.getEndAt())
                .remainSeconds(remain)
                .remainText(remainText(remain))
                .minCount(g.getMinCount())
                .maxCount(g.getMaxCount())
                .currentCount(currentCount)
                .progress(progress(currentCount, g.getMinCount()))
                .options(options)
                .build();
    }

    private GroupBuySummaryResponse toSummary(GroupBuy g, LocalDateTime now, boolean scheduled) {
        // 예정(SCHEDULED)은 '시작까지', 진행중은 '마감까지' 남은시간을 보여준다.
        long remain = remainSeconds(scheduled ? g.getStartAt() : g.getEndAt(), now);
        int currentCount = countParticipating(g.getSeq());
        return GroupBuySummaryResponse.builder()
                .seq(g.getSeq())
                .status(g.getStatus())
                .productName(g.getProduct().getProductName())
                .image(thumbnailUrl(g.getProduct().getSeq()))
                .originalPrice(g.getOriginalPrice())
                .finalPrice(g.getFinalPrice())
                .discountRate(discountRate(g.getOriginalPrice(), g.getFinalPrice()))
                .remainSeconds(remain)
                .remainText(remainText(remain))
                .minCount(g.getMinCount())
                .currentCount(currentCount)
                .progress(progress(currentCount, g.getMinCount()))
                .build();
    }

    /** 현재 정규 참여 인원 = 해당 공구의 PARTICIPATING 상태 participation 수. */
    private int countParticipating(Long groupBuySeq) {
        return (int) participationRepository.countByGroupBuySeqAndStatus(
                groupBuySeq, ParticipationStatus.PARTICIPATING);
    }

    /** 진행률(%) = 현재 참여 인원 / 최소 성사 인원 * 100. 최소 인원이 0/누락이면 0, 100% 초과는 100으로 고정. */
    private int progress(int currentCount, Integer minCount) {
        if (minCount == null || minCount <= 0) {
            return 0;
        }
        int p = (int) Math.round(currentCount * 100.0 / minCount);
        return Math.min(p, 100);
    }

    /** 남은 초를 "N일 N시간" 식 표기로. 마감 지났으면 "마감". */
    private String remainText(long seconds) {
        if (seconds <= 0) {
            return "마감";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) {
            return days + "일 " + hours + "시간";
        }
        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        }
        return minutes + "분";
    }

    /** 상품의 대표 썸네일(thumbnail_yn='Y') 이미지 URL. 없으면 기본 이미지. */
    private String thumbnailUrl(Long productSeq) {
        return productImageRepository
                .findFirstByProductSeqAndThumbnailYnAndStatus(productSeq, "Y", "NORMAL")
                .map(ProductImage::getImageUrl)
                .orElse(DEFAULT_IMAGE);
    }

    /** 할인율(%) = (정가 - 할인가) / 정가 * 100, 반올림. 정가가 0이면 0. */
    private Integer discountRate(Integer originalPrice, Integer finalPrice) {
        if (originalPrice == null || finalPrice == null || originalPrice <= 0) {
            return 0;
        }
        return (int) Math.round((originalPrice - finalPrice) * 100.0 / originalPrice);
    }

    /** 마감까지 남은 초. 이미 지났으면 0. */
    private Long remainSeconds(LocalDateTime endAt, LocalDateTime now) {
        if (endAt == null) {
            return 0L;
        }
        long seconds = Duration.between(now, endAt).getSeconds();
        return seconds > 0 ? seconds : 0L;
    }
}
