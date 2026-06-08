package com.example.java.mypage.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.mypage.dto.MyPageOrderDto;
import com.example.java.mypage.repository.MyPageQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MyPageQueryRepository myPageQueryRepository;

    public List<MyPageOrderDto> getOrders(Long memberSeq, String keyword) {
        return myPageQueryRepository.findOrdersByMemberSeq(memberSeq, keyword);
    }
}
