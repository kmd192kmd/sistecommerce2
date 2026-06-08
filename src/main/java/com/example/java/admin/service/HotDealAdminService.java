package com.example.java.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.admin.dto.HotDealRequestDto;
import com.example.java.admin.hotdeal.Entity.HotDeal;
import com.example.java.admin.repository.HotDealRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HotDealAdminService {

    private final HotDealRepository hotDealRepository;

    /**
     * 관리자: 신규 핫딜 생성
     */
    @Transactional
    public Long createHotDeal(HotDealRequestDto requestDto) {
        
        // 1. DTO -> Entity 변환
        // DTO 내부의 toEntity() 메서드가 최초 상태를 '0(대기)'로 고정하여 안전하게 변환해 줍니다.
        HotDeal hotDeal = requestDto.toEntity();

        // 2. 데이터베이스 저장
        // 데이터베이스 단의 CHK_HOTDEAL_DISCOUNT 제약 조건이 2차 방어선으로 한 번 더 작동하여 데이터를 굳건하게 지킵니다.
        HotDeal savedHotDeal = hotDealRepository.save(hotDeal);

        // 3. 성공적으로 생성된 핫딜의 고유번호(seq)를 반환하여 Controller가 응답할 수 있게 합니다.
        return savedHotDeal.getSeq();
    }
}