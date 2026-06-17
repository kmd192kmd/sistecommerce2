package com.example.java.admin.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.admin.dto.HotDealRequestDto;
import com.example.java.admin.hotdeal.Entity.HotDeal;
import com.example.java.admin.hotdeal.Entity.HotDealProduct;
import com.example.java.admin.repository.AdminOptionsRepository;
import com.example.java.admin.repository.HotDealProductRepository;
import com.example.java.admin.repository.HotDealRepository;
import com.example.java.product.entity.Options;
import com.example.java.stockhistory.enums.StockHistorySourceType;
import com.example.java.stockhistory.service.StockHistoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotDealAdminService {

    private final HotDealRepository hotDealRepository;
    private final HotDealProductRepository hotDealProductRepository;
    private final AdminOptionsRepository adminOptionsRepository;
    private final StockHistoryService stockHistoryService; 

    /**
     * 관리자: 신규 핫딜 생성
     */
    @Transactional
    public Long createHotDeal(HotDealRequestDto requestDto) {
        HotDeal hotDeal = requestDto.toEntity();
        HotDeal savedHotDeal = hotDealRepository.save(hotDeal);

        // 💡 List 배열로 넘어온 여러 옵션을 for문으로 돌면서 모두 처리합니다
        if (requestDto.getOptionsSeqs() != null && requestDto.getHotDealStocks() != null) {
            for (int i = 0; i < requestDto.getOptionsSeqs().size(); i++) {
                Long optSeq = requestDto.getOptionsSeqs().get(i);
                Integer stock = requestDto.getHotDealStocks().get(i);

                Options option = adminOptionsRepository.findById(optSeq)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션입니다."));

                // 재고이력 추가
                stockHistoryService.createOutStockHistory(
                		option, stock,
                		StockHistorySourceType.핫딜, "핫딜 등록으로 인한 선반영");
                
                option.decreaseStock(stock);

                HotDealProduct hotDealProduct = HotDealProduct.builder()
                        .hotDeal(savedHotDeal)
                        .options(option)
                        .hotDealStock(stock)
                        .soldQuantity(0)
                        .build();
                hotDealProductRepository.save(hotDealProduct);
                
               
            }
        }
        return savedHotDeal.getSeq();
    }

    @Transactional
    public void updateHotDeal(HotDealRequestDto requestDto) {
        HotDeal hotDeal = hotDealRepository.findById(requestDto.getSeq())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핫딜입니다."));

        if (hotDeal.getStatus() != 0) {
            throw new IllegalStateException("대기 상태인 핫딜만 수정할 수 있습니다.");
        }

        // 1. 핫딜 기본 정보 업데이트
        hotDeal.update(
                requestDto.getName(),
                requestDto.getStartDate().withMinute(0).withSecond(0).withNano(0),
                requestDto.getEndDate().withMinute(0).withSecond(0).withNano(0),
                requestDto.getDiscountRate(),
                requestDto.getDiscountPrice()
            );

        // 2. 기존 상품 목록을 Map으로 구성 (옵션번호 -> 기존 상품)
        List<HotDealProduct> existingProducts = hotDealProductRepository.findByHotDeal_Seq(hotDeal.getSeq());
        java.util.Map<Long, HotDealProduct> existingProductMap = existingProducts.stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.getOptions().getSeq(), p -> p));

        // 3. 들어온 요청(새로운 상태)과 기존 상태 비교 (Diff)
        if (requestDto.getOptionsSeqs() != null && requestDto.getHotDealStocks() != null) {
            for (int i = 0; i < requestDto.getOptionsSeqs().size(); i++) {
                Long optSeq = requestDto.getOptionsSeqs().get(i);
                Integer newStock = requestDto.getHotDealStocks().get(i);

                if (existingProductMap.containsKey(optSeq)) {
                    // [경우 A] 기존에 있던 옵션인 경우 -> 변동분만 계산
                    HotDealProduct existingProduct = existingProductMap.get(optSeq);
                    Integer oldStock = existingProduct.getHotDealStock();

                    int diff = newStock - oldStock;
                    if (diff > 0) {
                    	stockHistoryService.createOutStockHistory(
                                existingProduct.getOptions(), diff,
                                StockHistorySourceType.핫딜, "핫딜 수정으로 인한 추가 차감"
                        );
                        // ✅ 2. 그 다음 실제 재고 차감 (70으로 변경)
                        existingProduct.getOptions().decreaseStock(diff);
                    }  else if (diff < 0) {
                        int returnStock = Math.abs(diff);
                        // ✅ 1. 이력 먼저 기록
                        stockHistoryService.createInStockHistory(
                                existingProduct.getOptions(), returnStock,
                                StockHistorySourceType.핫딜, "핫딜 수정으로 인한 일부 재고 복구"
                        );
                        // ✅ 2. 그 다음 실제 재고 복구
                        existingProduct.getOptions().increaseStock(returnStock);
                    }

                    // 핫딜 상품 배정수량 업데이트
                    existingProduct.updateHotDealStock(newStock);

                    // 처리가 끝난 옵션은 Map에서 제거
                    existingProductMap.remove(optSeq);
                } else {
                    // [경우 B] 새로 추가된 옵션인 경우 -> 전체 차감
                    Options newOption = adminOptionsRepository.findById(optSeq)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 옵션입니다."));

                    newOption.decreaseStock(newStock);
                    stockHistoryService.createOutStockHistory(
                            newOption, newStock,
                            StockHistorySourceType.핫딜, "핫딜 수정으로 인한 신규 차감"
                    );

                    HotDealProduct newHotDealProduct = HotDealProduct.builder()
                            .hotDeal(hotDeal)
                            .options(newOption)
                            .hotDealStock(newStock)
                            .soldQuantity(0)
                            .build();
                    hotDealProductRepository.save(newHotDealProduct);
                }
            }
        }

        // 4. Map에 남아있는 상품들은 이번 수정에서 제외(삭제)된 옵션들이므로 전체 재고 원상복구
        for (HotDealProduct removedProduct : existingProductMap.values()) {
            // ✅ 1. 이력 먼저 기록
            stockHistoryService.createInStockHistory(
                    removedProduct.getOptions(), removedProduct.getHotDealStock(),
                    StockHistorySourceType.핫딜, "핫딜 수정으로 인한 제외 항목 복구"
            );
            // ✅ 2. 그 다음 실제 복구
            removedProduct.getOptions().increaseStock(removedProduct.getHotDealStock());
            hotDealProductRepository.delete(removedProduct);
        }
    }

    @Transactional(readOnly = true)
    public List<HotDeal> getAllHotDeals() {
        return hotDealRepository.findAll(Sort.by(Sort.Direction.DESC, "seq"));
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> searchOptionsSafely(String keyword) {
        List<Options> options = adminOptionsRepository.searchByProductName(keyword);
        return options.stream().map(opt ->
            java.util.Map.of(
                "seq", opt.getSeq(),
                "stock", opt.getStock(),
                "product", java.util.Map.of("productName", opt.getProduct().getProductName()),
                "color", opt.getColor() != null ? opt.getColor() : ""
            )
        ).collect(java.util.stream.Collectors.toList());
    }
    
    @Transactional
    public void deleteHotDeal(Long seq) {
        HotDeal hotDeal = hotDealRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핫딜입니다."));

        // 1. 대기 상태(0)가 아니면 삭제 차단
        if (hotDeal.getStatus() != 0) {
            throw new IllegalStateException("대기 상태인 핫딜만 삭제할 수 있습니다.");
        }

        // 2. 핫딜에 묶여있던 상품들 조회 후 재고 원상복구
        List<HotDealProduct> products = hotDealProductRepository.findByHotDeal_Seq(seq);
        for (HotDealProduct product : products) {
            Options option = product.getOptions();            
            
            stockHistoryService.createInStockHistory(
                    option, product.getHotDealStock(),
                    StockHistorySourceType.핫딜, "핫딜 삭제로 인한 재고 원상복구"
            );
            
            option.increaseStock(product.getHotDealStock()); // 뺏던 재고를 다시 더해줌
            
        }

        // 3. 매핑 데이터 및 핫딜 본체 삭제
        hotDealProductRepository.deleteAll(products);
        hotDealRepository.delete(hotDeal);
    }
    
    @Transactional(readOnly = true)
    public HotDeal getHotDeal(Long seq) {
        return hotDealRepository.findById(seq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 핫딜입니다."));
    }

    @Transactional(readOnly = true)
    public List<HotDealProduct> getHotDealProducts(Long hotDealSeq) {
        List<HotDealProduct> products = hotDealProductRepository.findByHotDeal_Seq(hotDealSeq);
        for (HotDealProduct p : products) {
        	

        	
            p.getOptions().getProduct().getProductName(); // 지연로딩 강제 초기화
            
        }
        return products;
    }

    

    @Transactional(readOnly = true)
    public String getHotDealSelectedOptionName(Long hotDealSeq) {
        List<HotDealProduct> products = hotDealProductRepository.findByHotDeal_Seq(hotDealSeq);
        if (products.isEmpty()) return null;

        Options opt = products.get(0).getOptions();
        // DB 트랜잭션이 열려있을 때 안전하게 Product 이름을 꺼냅니다.
        String pName = opt.getProduct().getProductName();
        String oName = opt.getColor() != null ? opt.getColor() : "기본옵션";

        return "[" + pName + "] " + oName;
    }
}