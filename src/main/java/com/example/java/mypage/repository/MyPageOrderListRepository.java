package com.example.java.mypage.repository;

import com.example.java.mypage.dto.MyPageOrderListDto;
import com.example.java.mypage.dto.MyPageCancelReturnDto;
import com.example.java.mypage.dto.MyPageOrderDetailDto;
import java.util.List;

public interface MyPageOrderListRepository {
    List<MyPageOrderListDto> findOrdersByMemberSeq(Long memberSeq, String keyword, String period);
    List<MyPageOrderListDto> findOrdersByMemberSeq(Long memberSeq, String keyword, String period, long offset, int size);
    
    List<MyPageCancelReturnDto> findCancelReturnsByMemberSeq(Long memberSeq);
    List<MyPageCancelReturnDto> findCancelReturnsByMemberSeq(Long memberSeq, long offset, int size);

    MyPageOrderDetailDto findOrderDetailByOrderSeq(Long orderSeq);
}


