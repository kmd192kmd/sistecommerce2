package com.example.java.groupbuy.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.groupbuy.dto.MyGroupBuyDto;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.entity.Participation;
import com.example.java.groupbuy.repository.ParticipationRepository;
import com.example.java.product.entity.Options;
import com.example.java.product.entity.Product;

import lombok.RequiredArgsConstructor;

/**
 * 마이페이지 공동구매 내역 서비스.
 * GroupBuyService(공구 참여·마감 처리 등)와 분리하여
 * 조회 전용 로직만 담습니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyGroupBuyService {

    private final ParticipationRepository participationRepository;

    /**
     * 로그인 회원의 공동구매 참여 내역을 최신순으로 반환합니다.
     */
    public List<MyGroupBuyDto> findByMember(Long memberSeq) {
        return participationRepository.findByMemberSeq(memberSeq)
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private MyGroupBuyDto toDto(Participation p) {
        GroupBuy groupBuy       = p.getGroupBuy();
        GroupBuyOptions gbOpts  = p.getGroupBuyOptions();
        Product product         = groupBuy.getProduct();
        Options options         = gbOpts.getOptions();

        String optionName = options.getDisplayName();
        if (optionName == null || optionName.isBlank()) {
            optionName = "기본 옵션";
        }

        return MyGroupBuyDto.builder()
                .participationSeq(p.getSeq())
                .groupBuySeq(groupBuy.getSeq())
                .productName(product.getProductName())
                .thumbnailUrl(product.getThumbnailUrl() != null
                        ? product.getThumbnailUrl()
                        : "/src/images/product/default.png")
                .optionName(optionName)
                .finalPrice(groupBuy.getFinalPrice())
                .startAt(groupBuy.getStartAt())
                .endAt(groupBuy.getEndAt())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
