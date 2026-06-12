package com.example.java.admin.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.admin.dto.GroupBuyAdminListDto;
import com.example.java.admin.dto.GroupBuyCreateDto;
import com.example.java.admin.dto.GroupBuyOptionDto;
import com.example.java.admin.dto.GroupBuySearchDto;
import com.example.java.admin.dto.ProductOptionInfoDto;
import com.example.java.admin.dto.SellerInfoDto;
import com.example.java.admin.repository.GroupBuyQueryDslRepository;
import com.example.java.groupbuy.entity.GroupBuy;
import com.example.java.groupbuy.entity.GroupBuyOptions;
import com.example.java.groupbuy.entity.GroupBuyStatus;
import com.example.java.groupbuy.entity.Participation;
import com.example.java.groupbuy.entity.ParticipationStatus;
import com.example.java.groupbuy.payment.GroupBuyPaymentPort;
import com.example.java.groupbuy.repository.GroupBuyOptionsRepository;
import com.example.java.groupbuy.repository.GroupBuyRepository;
import com.example.java.groupbuy.repository.ParticipationRepository;
import com.example.java.product.dto.ProductDto;
import com.example.java.product.entity.Options;
import com.example.java.product.entity.Product;
import com.example.java.product.repository.OptionsRepository;
import com.example.java.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupBuyAdminService {

    private final GroupBuyQueryDslRepository repository;
    private final GroupBuyRepository groupBuyRepository;
    private final GroupBuyOptionsRepository groupBuyOptionsRepository;
    private final ProductRepository productRepository;
    private final OptionsRepository optionsRepository;
    
    // 강제 중지 시 환불 및 참여 취소 처리를 위해 추가로 주입
    private final ParticipationRepository participationRepository;
    private final GroupBuyPaymentPort paymentPort;

    public GroupBuyCreateDto getCreateForm(List<Long> optionSeqs) {

        List<Options> optionsList =
                repository.getOptions(optionSeqs);

        if (optionsList.isEmpty()) {
            throw new IllegalArgumentException("선택된 옵션이 없습니다.");
        }

        Options firstOption = optionsList.get(0);
        Product product = firstOption.getProduct();

        SellerInfoDto sellerInfo =
                repository.getSellerInfo(product.getSellerSeq());

        Integer supplyPrice =
                product.getPrice()
                * sellerInfo.getSupplyRate() / 100;

        List<GroupBuyOptionDto> optionDtos =
                optionsList.stream()
                        .map(option -> GroupBuyOptionDto.builder()
                                .optionSeq(option.getSeq())
                                .optionName(option.getDisplayName())
                                .additionalPrice(option.getAdditionalPrice())
                                .build())
                        .toList();

        return GroupBuyCreateDto.builder()
                .productName(product.getProductName())
                .sellerName(sellerInfo.getSellerName())
                .sellerPhone(sellerInfo.getSellerPhone())
                .supplyPrice(supplyPrice)
                .options(optionDtos)
                .productSeq(product.getSeq())
                .originalPrice(product.getPrice())
                .build();
    }
    
    @Transactional
    public Long create(GroupBuyCreateDto dto) {
    	Product product = productRepository.findById(dto.getProductSeq()).get();

        GroupBuy groupBuy = GroupBuy.builder()
                .product(product)
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .minCount(dto.getMinCount())
                .maxCount(dto.getMaxCount())
                .createdAt(LocalDateTime.now())
                .originalPrice(dto.getOriginalPrice())
                .finalPrice(dto.getFinalPrice())
                .status(GroupBuyStatus.SCHEDULED)
                .build();

        groupBuyRepository.save(groupBuy);

        for (GroupBuyOptionDto option : dto.getOptions()) {
        	Options options = optionsRepository.findById(option.getOptionSeq()).get();

            GroupBuyOptions groupBuyOption =
                    GroupBuyOptions.builder()
                            .groupBuy(groupBuy)
                            .options(options)
                            .orderQty(option.getOrderQty())
                            .occupiedCount(0)
                            .build();

            groupBuyOptionsRepository.save(groupBuyOption);
        }

        return groupBuy.getSeq();
    }

    /**
     * Retrieve a page of products with option and pricing information for the admin select view.
     * Includes seller name, price display (single option or range), and flag for group buy.
     */
    @Transactional(readOnly = true)
    public Page<ProductOptionInfoDto> getProductOptionsPage(Pageable pageable) {
        // Calculate offset and size for repository pagination
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        // Fetch product DTOs (basic info) with paging
        List<ProductDto> productDtos = productRepository.findProductsByPaging(offset, size);
        // Total count for pagination metadata
        long total = productRepository.countProducts();

        List<ProductOptionInfoDto> result = productDtos.stream().map(p -> {
            // Seller information
            SellerInfoDto sellerInfo = repository.getSellerInfo(p.getSellerSeq());
            // Fetch all active options for the product
            Page<Options> optionsPage = optionsRepository.findByProductSeq(p.getSeq(), Pageable.unpaged());
            List<Options> options = optionsPage.getContent();

            int basePrice = p.getPrice();
            String priceDisplay;
            if (options.isEmpty()) {
                priceDisplay = String.format("%,d원", basePrice);
            } else if (options.size() == 1) {
                int add = options.get(0).getAdditionalPrice() != null ? options.get(0).getAdditionalPrice() : 0;
                priceDisplay = String.format("%,d원", basePrice + add);
            } else {
                int min = options.stream()
                        .mapToInt(o -> basePrice + (o.getAdditionalPrice() != null ? o.getAdditionalPrice() : 0))
                        .min().orElse(basePrice);
                int max = options.stream()
                        .mapToInt(o -> basePrice + (o.getAdditionalPrice() != null ? o.getAdditionalPrice() : 0))
                        .max().orElse(basePrice);
                priceDisplay = String.format("%,d원 ~ %,d원", min, max);
            }

            ProductOptionInfoDto dto = new ProductOptionInfoDto();
            dto.setProductSeq(p.getSeq());
            dto.setProductName(p.getProductName());
            dto.setSellerName(sellerInfo != null ? sellerInfo.getSellerName() : "");
            dto.setPriceDisplay(priceDisplay);
            dto.setHasOptions(!options.isEmpty());
            // Default to false; can be set later if needed
            dto.setGroupBuy(false);
            return dto;
        }).toList();

        return new PageImpl<>(result, pageable, total);
    }

    // --- 옵션 리스트 API ---
    // 이미 구현된 getOptionsByProduct 메서드 사용

    // --- 기타 기존 메서드 ---
    public Page<GroupBuyOptionDto> getOptionsByProduct(Long productSeq, Pageable pageable) {
        Page<Options> optionPage = optionsRepository.findByProductSeq(productSeq, pageable);
        List<GroupBuyOptionDto> dtos = optionPage.stream()
                .map(opt -> GroupBuyOptionDto.builder()
                        .optionSeq(opt.getSeq())
                        .optionName(opt.getDisplayName())
                        .additionalPrice(opt.getAdditionalPrice())
                        .build())
                .toList();
        return new PageImpl<>(dtos, pageable, optionPage.getTotalElements());
    }

	public Page<GroupBuyAdminListDto> searchGroupBuys(GroupBuySearchDto searchDto, Pageable pageable) {
	    return repository.searchGroupBuys(searchDto, pageable);
	}

// --- 추가하는 일괄 강제 중지 서비스 로직 ---
    @Transactional
    public void batchStop(List<Long> seqs) {
        if (seqs == null || seqs.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<GroupBuy> groupBuys = groupBuyRepository.findAllById(seqs);

        for (GroupBuy gb : groupBuys) {
            // 진행 중 또는 시작 대기 상태인 공동구매만 강제 중지 처리
            if (gb.getStatus() == GroupBuyStatus.ONGOING || gb.getStatus() == GroupBuyStatus.SCHEDULED) {
                gb.forceStop(now);

                // 1) 결제 완료 참여자 환불 및 무산 처리
                List<Participation> participations = participationRepository
                        .findByGroupBuySeqAndStatus(gb.getSeq(), ParticipationStatus.PARTICIPATING);
                
                for (Participation p : participations) {
                    // 한 명의 PG 환불 실패가 나머지 강제중단을 막지 않도록 격리한다 (NFR-006).
                    try {
                        paymentPort.refund(p.getSeq()); // 환불액은 order_item.final_price 스냅샷 사용(역산 X)
                    } catch (Exception e) {
                        log.error("[공구 강제중단 환불] 환불 실패 participationSeq={} (중단은 계속 진행)", p.getSeq(), e);
                    }
                    p.fail();
                }

                // 2) 결제 대기 상태 참여자 만료 처리
                participationRepository
                        .findByGroupBuySeqAndStatus(gb.getSeq(), ParticipationStatus.PAYMENT_PENDING)
                        .forEach(Participation::expire);
            }
        }
    }
}