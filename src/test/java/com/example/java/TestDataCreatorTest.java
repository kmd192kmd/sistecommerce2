package com.example.java;

import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.orders.entity.OrderItem;
import com.example.java.orders.entity.Orders;
import com.example.java.orders.entity.Payment;
import com.example.java.orders.repository.OrderItemRepository;
import com.example.java.orders.repository.OrdersRepository;
import com.example.java.orders.repository.PaymentRepository;
import com.example.java.product.entity.Options;
import com.example.java.product.entity.Seller;
import com.example.java.product.repository.OptionsRepository;
import com.example.java.product.repository.SellerRepository;
import com.example.java.delivery.entity.Delivery;
import com.example.java.delivery.entity.DeliveryCompany;
import com.example.java.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Disabled("Manual DB test data creator. Do not run during the default build.")
@SpringBootTest
public class TestDataCreatorTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OptionsRepository optionsRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Test
    @Transactional
    @Rollback(false) // Commit the created test data to the database
    public void createTestDataForQazwsx123() {
        Member member = memberRepository.findByUsername("qazwsx123")
                .orElseThrow(() -> new IllegalArgumentException("qazwsx123 회원을 찾을 수 없습니다."));

        // Clean up previous test data to avoid duplicates/errors
        System.out.println("=== 기존 테스트 데이터 정리 시작 ===");
        List<Orders> oldOrders = ordersRepository.findAll().stream()
                .filter(o -> o.getOrderUid() != null && o.getOrderUid().startsWith("TEST-ORD-"))
                .toList();
        for (Orders o : oldOrders) {
            deliveryRepository.deleteAll(deliveryRepository.findByOrders_Seq(o.getSeq()));
            orderItemRepository.deleteAll(orderItemRepository.findByOrderSeq(o.getSeq()));
            List<Payment> oldPayments = paymentRepository.findAll().stream()
                    .filter(p -> p.getOrderSeq().equals(o.getSeq()))
                    .toList();
            paymentRepository.deleteAll(oldPayments);
            ordersRepository.delete(o);
        }
        System.out.println("=== 기존 테스트 데이터 정리 완료 ===");

        Options option = optionsRepository.findAll().stream()
                .filter(o -> o.getProduct() != null && o.getProduct().getSellerSeq() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("판매처가 연동된 상품 옵션 데이터가 없습니다."));

        Seller seller = sellerRepository.findById(option.getProduct().getSellerSeq())
                .orElseThrow(() -> new IllegalStateException("판매자 정보를 찾을 수 없습니다."));

        DeliveryCompany company = seller.getDeliveryCompany();
        if (company == null) {
            throw new IllegalStateException("판매자와 매핑된 배송 택배사 정보가 없습니다.");
        }

        System.out.println("=== qazwsx123 회원 테스트 데이터 생성 시작 ===");
        System.out.println("사용할 상품 옵션: " + option.getOptionsType() + " (Product: " + option.getProduct().getProductName() + ")");
        System.out.println("사용할 택배사: " + company.getName());

        for (int i = 1; i <= 5; i++) {
            String uid = "TEST-ORD-" + System.currentTimeMillis() + "-" + i;

            // 1. Orders 생성 (orderStatus=6: 배송완료, paymentStatus=2: 결제완료)
            Orders order = Orders.builder()
                    .memberSeq(member.getSeq())
                    .orderUid(uid)
                    .productTotalPrice(50000)
                    .couponDiscount(0)
                    .hotdealDiscount(0)
                    .finalPrice(50000)
                    .totalRefundPrice(0)
                    .remainPrice(50000)
                    .orderStatus(6) // 6: 배송완료
                    .paymentStatus(2) // 2: 결제완료
                    .orderDate(LocalDateTime.now().minusDays(1))
                    .regdate(LocalDateTime.now().minusDays(1))
                    .zipcode("12345")
                    .address("테스트 주소")
                    .addressDetail("테스트 상세주소 " + i)
                    .currLatitude(37.5665)
                    .currLongitude(126.978)
                    .build();

            Orders savedOrder = ordersRepository.save(order);

            // 2. OrderItem 생성 (itemStatus=0: 정상)
            OrderItem item = OrderItem.builder()
                    .orderSeq(savedOrder.getSeq())
                    .optionsSeq(option.getSeq())
                    .productName(option.getProduct().getProductName())
                    .quantity(1)
                    .originalPrice(50000)
                    .finalPrice(50000)
                    .subTotalPrice(50000)
                    .itemStatus(0)
                    .build();

            orderItemRepository.save(item);

            // 3. Payment 생성 (status=2: 결제완료)
            String payUid = "TEST-PAY-" + System.currentTimeMillis() + "-" + i;
            Payment payment = Payment.builder()
                    .orderSeq(savedOrder.getSeq())
                    .paymentUid(payUid)
                    .externalPaymentId("mock_payment_key_" + i + "_" + System.currentTimeMillis())
                    .pgTid("mock_pg_tid_" + i + "_" + System.currentTimeMillis())
                    .paymentMethod(1)
                    .pgProvider("TOSS")
                    .status(2) // 2: 결제완료
                    .amount(50000)
                    .requestDate(LocalDateTime.now().minusDays(1))
                    .payDate(LocalDateTime.now().minusDays(1))
                    .updateDate(LocalDateTime.now().minusDays(1))
                    .build();

            paymentRepository.save(payment);

            // 4. Delivery 생성 (status='DELIVERED')
            String trackingNumber = "TEST-TRK-" + System.currentTimeMillis() + "-" + i;
            Delivery delivery = Delivery.builder()
                    .tracking_number(trackingNumber)
                    .recipient_name(member.getName() != null ? member.getName() : "테스터")
                    .recipient_phone(member.getPhone() != null ? member.getPhone() : "010-1234-5678")
                    .status("DELIVERED") // 배송완료
                    .request_memo("조심히 배송해 주세요.")
                    .dispatch_at(LocalDateTime.now().minusDays(2))
                    .estimated_date(LocalDateTime.now().minusDays(1))
                    .completed_at(LocalDateTime.now().minusDays(1))
                    .distance_surcharge(0)
                    .total_delivery_fee(3000)
                    .deliveryCompany(company)
                    .orders(savedOrder)
                    .delayHours(0)
                    .build();

            deliveryRepository.save(delivery);
            System.out.println("주문 데이터 생성 완료 (" + i + "/5): OrderUid=" + uid);
        }
        System.out.println("=== qazwsx123 회원 테스트 데이터 생성 완료 ===");
    }

    @Test
    @Transactional
    public void checkTestData() {
        System.out.println("=== [START] DB 테스트 데이터 조회 검증 ===");
        Member member = memberRepository.findByUsername("qazwsx123")
                .orElse(null);
        if (member == null) {
            System.out.println("qazwsx123 회원을 찾을 수 없습니다.");
            return;
        }

        List<Orders> ordersList = ordersRepository.findAll().stream()
                .filter(o -> o.getMemberSeq().equals(member.getSeq()))
                .toList();

        System.out.println("qazwsx123 회원의 주문 개수: " + ordersList.size());
        for (Orders order : ordersList) {
            System.out.println("주문 UID: " + order.getOrderUid() + ", Seq: " + order.getSeq() + ", Status: " + order.getOrderStatus());
            
            // Payment 조회 테스트
            Payment payment = paymentRepository.findTopByOrderSeqAndStatusOrderBySeqDesc(order.getSeq(), 2).orElse(null);
            if (payment != null) {
                System.out.println("  -> 결제 정보 발견: Seq=" + payment.getSeq() + ", PaymentUid=" + payment.getPaymentUid() + ", Status=" + payment.getStatus());
            } else {
                System.out.println("  -> 결제 정보 없음!");
            }

            // Delivery 조회 테스트
            List<Delivery> deliveries = deliveryRepository.findByOrders_Seq(order.getSeq());
            System.out.println("  -> 배송 개수: " + deliveries.size());
            for (Delivery d : deliveries) {
                System.out.println("     -> 배송 Seq: " + d.getSeq() + ", Status: " + d.getStatus() + ", Carrier: " + (d.getDeliveryCompany() != null ? d.getDeliveryCompany().getName() : "None"));
            }
        }
        System.out.println("=== [END] DB 테스트 데이터 조회 검증 ===");
    }
}



