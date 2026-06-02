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
import com.example.java.groupbuy.repository.GroupBuyOptionsRepository;
import com.example.java.groupbuy.repository.GroupBuyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GroupBuyService {
	
    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionsRepository groupBuyOptionsRepository;

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
                .productSeq(dto.getProductSeq())
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
                    .optionsSeq(od.getOptionsSeq())
                    .orderQty(od.getOrderQty())
                    .occupiedCount(0)
                    .build();
            groupBuyOptionsRepository.save(option);
        }

        return saved.getSeq();
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

        return GroupBuyDetailResponse.builder()
                .seq(g.getSeq())
                .status(g.getStatus())
                .originalPrice(g.getOriginalPrice())
                .finalPrice(g.getFinalPrice())
                .discountRate(discountRate(g.getOriginalPrice(), g.getFinalPrice()))
                .startAt(g.getStartAt())
                .endAt(g.getEndAt())
                .remainSeconds(remainSeconds(g.getEndAt(), LocalDateTime.now()))
                .minCount(g.getMinCount())
                .maxCount(g.getMaxCount())
                .options(options)
                .build();
    }

    private GroupBuySummaryResponse toSummary(GroupBuy g, LocalDateTime now) {
        return GroupBuySummaryResponse.builder()
                .seq(g.getSeq())
                .status(g.getStatus())
                .originalPrice(g.getOriginalPrice())
                .finalPrice(g.getFinalPrice())
                .discountRate(discountRate(g.getOriginalPrice(), g.getFinalPrice()))
                .remainSeconds(remainSeconds(g.getEndAt(), now))
                .minCount(g.getMinCount())
                .build();
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
