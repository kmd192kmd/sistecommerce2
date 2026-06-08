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
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.entity.GroupBuyStatus;
import com.example.java.groupbuy.entity.Participation;
import com.example.java.groupbuy.entity.ParticipationStatus;
import com.example.java.groupbuy.payment.GroupBuyPaymentPort;
import com.example.java.groupbuy.repository.GroupBuyOptionsRepository;
import com.example.java.groupbuy.repository.GroupBuyRepository;
import com.example.java.groupbuy.repository.ParticipationRepository;
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
    private final GroupBuyPaymentPort paymentPort;

    // ProductRepository가 dev에서 class(EntityManager 직접 구현)로 바뀌어 JpaRepository 메서드가 없음.
    // 등록 시 Product FK만 세팅하면 되므로 EntityManager.getReference로 프록시 참조를 얻는다.
    @PersistenceContext
    private EntityManager entityManager;

    /** 썸네일 이미지가 없을 때 사용할 기본 이미지 (static 리소스). */
    private static final String DEFAULT_IMAGE = "/src/images/product/default.png";

    /** 중복참여 판정 대상 = 진행 중인 참여 상태. 이 상태가 있으면 같은 공구 재참여를 막는다. */
    private static final List<ParticipationStatus> ACTIVE_PARTICIPATION_STATUSES =
            List.of(ParticipationStatus.PARTICIPATING, ParticipationStatus.PAYMENT_PENDING);

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
     * 공구 정규 참여 (1단계: 성공 경로).
     *
     * 흐름:
     *  1) findBySeqForUpdate 로 옵션 행에 비관적 쓰기 락(SELECT ... FOR UPDATE) → 같은 옵션 경쟁을 줄세움
     *  2) 옵션-공구 일치 + 공구 진행상태(ONGOING)/기간 검증
     *  3) 중복참여 검사 (진행 중 참여가 있으면 거부, 취소 후 재참여는 허용)
     *  4) occupy() 로 점유(+1) — 매진이면 예외 (대기열 승격은 다음 단계)
     *  5) 결제(현재 스텁, 항상 성공) — 실패 시 예외로 트랜잭션 롤백 → 점유도 원복
     *  6) participation INSERT (PARTICIPATING)
     *
     * 옵션 행 락이 commit까지 유지되므로 동시 참여가 직렬화되어 orderQty(=옵션별 정원) 초과가 차단된다 (NFR-001).
     *
     * @param groupBuySeq 참여할 공구 seq
     * @param optionSeq   선택한 group_buy_options.seq
     * @param memberSeq   참여 회원 seq
     */
    @Transactional
    public void participate(Long groupBuySeq, Long optionSeq, Long memberSeq) {
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

        // 3) 중복참여 검사: 같은 공구에 진행 중(PARTICIPATING/PAYMENT_PENDING)인 참여가 있으면 거부
        if (participationRepository.existsByGroupBuySeqAndMemberSeqAndStatusIn(
                groupBuySeq, memberSeq, ACTIVE_PARTICIPATION_STATUSES)) {
            throw new IllegalStateException("이미 이 공동구매에 참여 중입니다.");
        }

        // 4) 옵션 점유(+1). 매진이면 예외 (대기열 승격은 다음 단계)
        option.occupy();
        // 명시적 save 불필요: 락으로 조회한 option은 영속 상태라 commit 시 변경감지로 UPDATE 됨.

        // 5) 결제(현재 스텁: 항상 성공). 실패 시 예외 → 트랜잭션 롤백으로 점유도 원복
        paymentPort.pay(memberSeq, groupBuy.getFinalPrice());

        // 6) 참여 INSERT (정규 참여라 paymentDeadline/promotedAt은 null)
        participationRepository.save(Participation.builder()
                .groupBuy(groupBuy)
                .groupBuyOptions(option)
                .memberSeq(memberSeq)
                .status(ParticipationStatus.PARTICIPATING)
                .createdAt(LocalDateTime.now())
                .build());
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
