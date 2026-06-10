package com.example.java.groupbuy.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

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
import com.example.java.groupbuy.payment.GroupBuyPaymentPort;
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
    public ParticipateResult participate(Long groupBuySeq, Long optionSeq, Long memberSeq) {
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
            //      정규 참여자가 이탈해 자리가 나면 FIFO(created_at)로 승격된다 (승격은 취소 흐름에서 처리, 다음 단계).
            waitingQueueRepository.save(WaitingQueue.builder()
                    .groupBuy(groupBuy)
                    .groupBuyOptions(option)
                    .memberSeq(memberSeq)
                    .createdAt(LocalDateTime.now())   // FIFO 승격 순서의 기준이 되는 등록 시각
                    .build());
            return ParticipateResult.QUEUED;
        }

        // 4-B) 정원 남음 → 정규 참여
        // 옵션 점유(+1). 위에서 isSoldOut()을 확인했으니 통과하지만, occupy()는 내부에서도
        // 매진을 재검사해 이중 방어한다(NFR-001).
        option.occupy();
        // 명시적 save 불필요: 락으로 조회한 option은 영속 상태라 commit 시 변경감지로 UPDATE 됨.

        // 결제(현재 스텁: 항상 성공). 실패 시 예외 → 트랜잭션 롤백으로 점유도 원복
        paymentPort.pay(memberSeq, groupBuy.getFinalPrice());

        // 참여 INSERT (정규 참여라 paymentDeadline/promotedAt은 null)
        participationRepository.save(Participation.builder()
                .groupBuy(groupBuy)
                .groupBuyOptions(option)
                .memberSeq(memberSeq)
                .status(ParticipationStatus.PARTICIPATING)
                .createdAt(LocalDateTime.now())
                .build());

        return ParticipateResult.PARTICIPATED;
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
        paymentPort.cancel(memberSeq, groupBuy.getFinalPrice());

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

                    participationRepository.save(Participation.builder()
                            .groupBuy(groupBuy)
                            .groupBuyOptions(option)
                            .memberSeq(next.getMemberSeq())
                            .status(ParticipationStatus.PAYMENT_PENDING)
                            .paymentDeadline(deadline)  // 승격자 결제 제한시각
                            .promotedAt(now)            // 승격된 시각
                            .createdAt(now)
                            .build());
                });
    }

    /**
     * 승격자 결제 (대기열에서 승격된 결제대기자가 기한 내 직접 결제).
     *
     * 승격 시 이미 옵션을 점유(occupied_count +1)했으므로, 이 메서드는 점유 수를 바꾸지 않고
     * 상태만 PAYMENT_PENDING → PARTICIPATING 으로 "확정"시킨다.
     * 결제가 완료돼야 비로소 확정 인원(PARTICIPATING 수)에 포함된다 (NFR-003 정합성).
     *
     * 흐름:
     *  1) 결제대기(PAYMENT_PENDING) 참여 조회 → 없으면 거부
     *  2) 옵션 행 락 → 만료 처리(스케줄러)·취소와 직렬화
     *  3) refresh로 상태 재확인 → 그 사이 만료(FAILED)되거나 이미 결제됐으면 거부 (중복 결제 방지)
     *  4) 공구 ONGOING + 결제기한(now ≤ payment_deadline) 검증
     *  5) 결제(스텁) — 실패 시 예외로 롤백
     *  6) 상태 전이 PAYMENT_PENDING → PARTICIPATING
     *
     * @param groupBuySeq 결제할 공구 seq
     * @param memberSeq   승격된 회원 seq
     */
    @Transactional
    public void confirmPromotedPayment(Long groupBuySeq, Long memberSeq) {
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

        // 5) 결제 (스텁: 항상 성공). 실패 시 예외 → 트랜잭션 롤백
        paymentPort.pay(memberSeq, groupBuy.getFinalPrice());

        // 6) 확정: PAYMENT_PENDING → PARTICIPATING. occupied_count는 이미 점유돼 있어 변동 없음.
        participation.confirmPayment();
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
        return groupBuyRepository.findAll().stream()
                .map(g -> toSummary(g, now))
                .toList();
    }

    /** 공구 상세 응답 (회원용). 옵션은 매진 여부만 노출. */
    @Transactional(readOnly = true)
    public GroupBuyDetailResponse getDetail(Long seq) {
        GroupBuy g = groupBuyRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("공구를 찾을 수 없습니다. seq=" + seq));

        List<GroupBuyOptionView> options = groupBuyOptionsRepository.findByGroupBuySeq(seq).stream()
                .map(GroupBuyOptionView::from)
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

    private GroupBuySummaryResponse toSummary(GroupBuy g, LocalDateTime now) {
        long remain = remainSeconds(g.getEndAt(), now);
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
