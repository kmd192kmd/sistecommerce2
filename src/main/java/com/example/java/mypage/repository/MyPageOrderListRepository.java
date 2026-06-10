package com.example.java.mypage.repository;

import com.example.java.mypage.dto.MyPageOrderListDto;
import java.util.List;

public interface MyPageOrderListRepository {
    List<MyPageOrderListDto> findOrdersByMemberSeq(Long memberSeq, String keyword, String period);
}
