package com.example.java.mypage.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.mypage.dto.MyPageOrderListDto;
import com.example.java.mypage.dto.MyPageCancelReturnDto;
import com.example.java.mypage.repository.MyPageOrderListRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageOrderListService {

    private final MyPageOrderListRepository myPageOrderListRepository;

    public List<MyPageOrderListDto> getOrders(Long memberSeq, String keyword, String period) {
        return myPageOrderListRepository.findOrdersByMemberSeq(memberSeq, keyword, period);
    }

    public List<MyPageCancelReturnDto> getCancelReturns(Long memberSeq) {
        return myPageOrderListRepository.findCancelReturnsByMemberSeq(memberSeq);
    }
}
