package com.example.java.mypage.repository;

import com.example.java.mypage.dto.MyPageOrderDto;
import java.util.List;

public interface MyPageQueryRepository {
    List<MyPageOrderDto> findOrdersByMemberSeq(Long memberSeq, String keyword);
}
