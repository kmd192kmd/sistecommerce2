package com.example.java.groupbuy.payment;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.orders.repository.OrdersRepository;
import com.example.java.orders.service.PaymentService;

/**
 * 공구 환불 어댑터 단위 검증 (실제 토스 없이 PaymentService 위임만 확인).
 * participation_seq → 결제완료 주문을 찾아 cancelPayment(orderSeq, memberSeq)에 위임하는지,
 * 이미 취소된 주문이면 환불을 생략(멱등)하는지 본다.
 */
@ExtendWith(MockitoExtension.class)
class GroupBuyRefundAdapterTest {

    @Mock OrderItemRepository orderItemRepository;
    @Mock OrdersRepository ordersRepository;
    @Mock PaymentService paymentService;

    @InjectMocks GroupBuyRefundAdapter adapter;

    @Test
    void 결제완료_주문을_찾아_토스취소에_위임한다() {
        // given: 참여 5번의 주문상품 → 주문 10번(회원 7번, 결제완료=2)
        OrderItem item = OrderItem.builder().orderSeq(10L).participationSeq(5L).build();
        when(orderItemRepository.findByParticipationSeq(5L)).thenReturn(List.of(item));
        Orders order = Orders.builder().seq(10L).memberSeq(7L).paymentStatus(2).build();
        when(ordersRepository.findById(10L)).thenReturn(Optional.of(order));

        // when
        adapter.refund(5L);

        // then: 그 주문을 orderSeq/memberSeq로 취소 위임
        verify(paymentService).cancelPayment(10L, 7L);
    }

    @Test
    void 이미_취소된_주문이면_환불을_생략한다() {
        // given: 주문이 이미 취소(payment_status=6)된 상태 → 결제완료(2) 아님
        OrderItem item = OrderItem.builder().orderSeq(10L).participationSeq(5L).build();
        when(orderItemRepository.findByParticipationSeq(5L)).thenReturn(List.of(item));
        Orders order = Orders.builder().seq(10L).memberSeq(7L).paymentStatus(6).build();
        when(ordersRepository.findById(10L)).thenReturn(Optional.of(order));

        // when
        adapter.refund(5L);

        // then: 중복 취소 호출 없음 (멱등, NFR-004)
        verify(paymentService, never()).cancelPayment(anyLong(), anyLong());
    }
}
