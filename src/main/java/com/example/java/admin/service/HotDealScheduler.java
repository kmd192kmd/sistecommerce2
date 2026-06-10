package com.example.java.admin.service;


import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.admin.hotdeal.Entity.HotDeal;
import com.example.java.admin.hotdeal.Entity.HotDealProduct;
import com.example.java.admin.repository.HotDealProductRepository;
import com.example.java.admin.repository.HotDealRepository;
import com.example.java.stockhistory.enums.StockHistorySourceType;
import com.example.java.stockhistory.service.StockHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotDealScheduler {

        private final HotDealRepository hotDealRepository;
        private final HotDealProductRepository hotDealProductRepository;
        private final StockHistoryService stockHistoryService;
        
        /**
         * 1분마다 주기적으로 실행되어 핫딜 상태와 재고를 자동으로 관리합니다.
         */
        @Scheduled(cron = "0 0 * * * *") // 매 분 0초마다 실행 (1분 주기)
        @Transactional
        public void autoManageHotDeals() {
            LocalDateTime now = LocalDateTime.now();

            // 1. 대기(0) -> 진행중(1) 상태 변경
            List<HotDeal> pendingDeals = hotDealRepository.findByStatusAndStartDateLessThanEqual(0, now);
            for (HotDeal deal : pendingDeals) {
                deal.updateStatus(1);
                log.info("🔥 [핫딜 오픈] 대기 -> 진행중: {}", deal.getName());
            }

            // 2. 진행중(1) -> 종료(2) 상태 변경 및 "남은 재고 원상복구" (제일 중요!)
            List<HotDeal> ongoingDeals = hotDealRepository.findByStatusAndEndDateLessThanEqual(1, now);
            for (HotDeal deal : ongoingDeals) {
                deal.updateStatus(2);/// 종료 상태로 변경

                // 핫딜 상품 재고 복구 (남은 재고 = 처음 핫딜용으로 뺀 재고 - 실제 팔린 수량)
                List<HotDealProduct> products = hotDealProductRepository.findByHotDeal_Seq(deal.getSeq());
                for (HotDealProduct product : products) {
                    // 팔린 수량이 null일 수 있으니 안전하게 0으로 처리
                    int soldQty = product.getSoldQuantity() == null ? 0 : product.getSoldQuantity();
                    int remainingStock = product.getHotDealStock() - soldQty;

                    // 안 팔리고 남은 재고가 있다면 다시 원래 상품(Options)으로 돌려줌
                    if (remainingStock > 0) {                        
                        
                        stockHistoryService.createInStockHistory(
                                product.getOptions(), remainingStock,
                                StockHistorySourceType.핫딜, "핫딜 종료로 인한 남은 재고 원상복구"
                        );
                        
                        product.getOptions().increaseStock(remainingStock);
                        
                        log.info("📦 [핫딜 마감 재고 복구] 상품명: {}, 복구된 수량: {}개",
                                 product.getOptions().getProduct().getProductName(), remainingStock);
                    }
                }
                log.info("🏁 [핫딜 마감] 진행중 -> 종료: {}", deal.getName());
            }
        }
        
        
    }
	
