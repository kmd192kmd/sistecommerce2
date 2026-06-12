package com.example.java.mypage.repository;

import com.example.java.mypage.dto.MyPageOrderListDto;
import com.example.java.mypage.dto.MyPageCancelReturnDto;
import java.util.List;

public interface MyPageOrderListRepository {
    List<MyPageOrderListDto> findOrdersByMemberSeq(Long memberSeq, String keyword, String period);
    
    List<MyPageCancelReturnDto> findCancelReturnsByMemberSeq(Long memberSeq);
}

