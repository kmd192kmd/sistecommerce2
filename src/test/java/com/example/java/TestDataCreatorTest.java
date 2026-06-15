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
import com.example.java.orders.entity.ReturnRequest;
import com.example.java.orders.entity.Returns;
import com.example.java.orders.entity.Refund;
import com.example.java.orders.repository.ReturnRequestRepository;
import com.example.java.orders.repository.ReturnsRepository;
import com.example.java.orders.repository.RefundRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.example.java.mypage.service.MyPageOrderListService;
import com.example.java.mypage.dto.MyPageOrderListDto;
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

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private ReturnsRepository returnsRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private MyPageOrderListService myPageOrderListService;

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
            List<OrderItem> items = orderItemRepository.findByOrderSeq(o.getSeq());
            for (OrderItem item : items) {
                List<ReturnRequest> reqs = returnRequestRepository.findAll().stream()
                        .filter(r -> r.getOrderItemSeq().equals(item.getSeq()))
                        .toList();
                for (ReturnRequest req : reqs) {
                    List<Returns> rets = returnsRepository.findAll().stream()
                            .filter(ret -> ret.getReturnRequest() != null && ret.getReturnRequest().getSeq().equals(req.getSeq()))
                            .toList();
                    returnsRepository.deleteAll(rets);
                    returnRequestRepository.delete(req);
                }
                List<Refund> refs = refundRepository.findAll().stream()
                        .filter(ref -> ref.getOrderItemSeq().equals(item.getSeq()))
                        .toList();
                refundRepository.deleteAll(refs);
            }
            deliveryRepository.deleteAll(deliveryRepository.findByOrders_Seq(o.getSeq()));
            orderItemRepository.deleteAll(items);
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

            String devStatus = "DELIVERED";
            int ordStatus = 6;
            if (i == 1) { devStatus = "READY"; ordStatus = 5; }
            else if (i == 2) { devStatus = "SHIPPING"; ordStatus = 5; }
            else if (i == 3) { devStatus = "DELAYED"; ordStatus = 5; }
            else if (i == 4) { devStatus = "FAILED"; ordStatus = 5; }

            // 1. Orders 생성
            Orders order = Orders.builder()
                    .memberSeq(member.getSeq())
                    .orderUid(uid)
                    .productTotalPrice(50000)
                    .couponDiscount(0)
                    .hotdealDiscount(0)
                    .finalPrice(50000)
                    .totalRefundPrice(0)
                    .remainPrice(50000)
                    .orderStatus(ordStatus)
                    .paymentStatus(2) // 2: 결제완료
                    .orderDate(LocalDateTime.now().minusDays(i))
                    .regdate(LocalDateTime.now().minusDays(i))
                    .zipcode("12345")
                    .address("테스트 주소")
                    .addressDetail("테스트 상세주소 (" + devStatus + ")")
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
                    .requestDate(LocalDateTime.now().minusDays(i))
                    .payDate(LocalDateTime.now().minusDays(i))
                    .updateDate(LocalDateTime.now().minusDays(i))
                    .build();

            paymentRepository.save(payment);

            // 4. Delivery 생성
            String trackingNumber = "TEST-TRK-" + System.currentTimeMillis() + "-" + i;
            Delivery delivery = Delivery.builder()
                    .tracking_number(trackingNumber)
                    .recipient_name(member.getName() != null ? member.getName() : "테스터")
                    .recipient_phone(member.getPhone() != null ? member.getPhone() : "010-1234-5678")
                    .status(devStatus)
                    .request_memo("조심히 배송해 주세요.")
                    .dispatch_at(LocalDateTime.now().minusDays(i + 1))
                    .estimated_date(LocalDateTime.now().minusDays(i))
                    .completed_at("DELIVERED".equals(devStatus) ? LocalDateTime.now().minusDays(i) : null)
                    .distance_surcharge(0)
                    .total_delivery_fee(3000)
                    .deliveryCompany(company)
                    .orders(savedOrder)
                    .delayHours(i == 3 ? 24 : 0)
                    .build();

            deliveryRepository.save(delivery);
            System.out.println("주문 데이터 생성 완료 (" + i + "/5): OrderUid=" + uid + " Status=" + devStatus);
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

        List<MyPageOrderListDto> orderDtos = myPageOrderListService.getOrders(member.getSeq(), null, "6months");
        System.out.println("myPageOrderListService.getOrders 개수: " + orderDtos.size());
        for (MyPageOrderListDto o : orderDtos) {
            System.out.println("주문 Seq: " + o.getOrderSeq() + ", allDelivered: " + o.isAllDelivered());
            for (var d : o.getDeliveries()) {
                System.out.println("  -> Delivery Group: Company=" + d.getCompanyName() + ", Status=" + d.getDeliveryStatus() + ", CompletedAt=" + d.getCompletedAt());
                for (var item : d.getItems()) {
                    System.out.println("     -> Item: Name=" + item.getName() + ", itemStatus=" + item.getItemStatus());
                }
            }
        }
        System.out.println("=== [END] DB 테스트 데이터 조회 검증 ===");
    }
}



