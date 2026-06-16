package com.example.java.groupbuy.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.groupbuy.dto.GroupBuyDetailResponse;
import com.example.java.groupbuy.dto.GroupBuySummaryResponse;
import com.example.java.groupbuy.dto.ParticipateResponse;
import com.example.java.groupbuy.service.GroupBuyService;
import com.example.java.member.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;

/**
 * 공동구매 REST API. (Thymeleaf shell 위에 mount된 React가 호출)
 * 회원 노출용 데이터만 제공 — 옵션 잔여 수량 등 비공개 정보는 내려보내지 않는다.
 */
@RestController
@RequestMapping("/api/group-buys")
@RequiredArgsConstructor
public class GroupBuyApiController {

    private final GroupBuyService groupBuyService;

    /** 공구 목록. */
    // GET /api/group-buys  (목록)
    @GetMapping
    public List<GroupBuySummaryResponse> list() {
        return groupBuyService.getSummaries();
    }

    /** 공구 상세. */
    // GET /api/group-buys/7  (상세)
    @GetMapping("/{seq}")
    public GroupBuyDetailResponse detail(@PathVariable(name = "seq") Long seq) {
        return groupBuyService.getDetail(seq);
    }

    /**
     * 공구 참여 신청 (정규 참여 또는 대기열 등록).
     * 예) POST /api/group-buys/7/participate?optionSeq=3  
     * → "7번 공구에 3번 옵션으로 참여 신청"
     *
     * 조회(GET)와 달리 서버 상태(점유·참여 기록·대기열)를 바꾸므로 POST를 쓴다.
     * 참여는 "누가" 하는지가 필수라, 로그인한 회원 정보를 함께 받는다.
     *
     * 정원이 남으면 자리를 예약(점유)하고 '결제 대기' 
     * 주문을 만들어 orderUid/amount를 함께 내려준다
     * → 프론트가 그 값으로 토스 결제창을 띄운다(2단계). 
     * 매진이면 대기열 등록(QUEUED, 결제 정보 없음).
     */
    @PostMapping("/{seq}/participate")
    public ResponseEntity<ParticipateResponse> participate(
            // @PathVariable: URL 경로의 {seq} 자리값을 꺼냄 (/group-buys/[7]/participate → seq=7)
            @PathVariable(name = "seq") Long seq,
            // @RequestParam: 쿼리스트링 ?optionSeq=3 의 값을 꺼냄 → 회원이 고른 옵션
            @RequestParam(name = "optionSeq") Long optionSeq,
            // @AuthenticationPrincipal: 지금 로그인한 사용자
            // (SecurityContext의 principal)를 자동으로 주입.
            // 타입을 CustomUserDetails로 받았으니 거기 담아둔 
            // getMemberSeq()를 바로 쓸 수 있다.
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            // 비로그인이면 principal이 없어 user가 null → 401(인증 필요)로 막는다.
            // (SecurityConfig가 현재 개발용 permitAll이라 
        	// 비로그인도 들어올 수 있어 여기서 직접 방어)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 검증·점유·자리예약·주문생성·대기열은 서비스(트랜잭션)에 위임. 
        // 컨트롤러는 입력만 꺼내 넘긴다.
        ParticipateResponse response = groupBuyService.participate(seq, optionSeq, user.getMemberSeq());
        // 결과를 JSON으로 반환 → QUEUED(대기열) 또는 
        // PARTICIPATED(+orderUid/amount로 토스 결제창)
        return ResponseEntity.ok(response);
    }

    /**
     * 공구 참여 취소.
     * 예) POST /api/group-buys/7/cancel  → "7번 공구 내 참여를 취소"
     *
     * 1인 1상품이라 공구 + 로그인 회원이면 취소 대상 참여가 유일하게 특정되므로, 
     * optionSeq는 받지 않는다.
     * 취소 → 환불 → 점유 복구 → 같은 옵션 대기열 FIFO 승격까지 
     * 서비스가 한 트랜잭션으로 처리한다.
     */
    @PostMapping("/{seq}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable(name = "seq") Long seq,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        groupBuyService.cancel(seq, user.getMemberSeq());
        return ResponseEntity.ok().build();
    }

    /**
     * 승격자 결제 시작.
     * 예) POST /api/group-buys/7/promotion-payment  
     * → "7번 공구에서 승격된 내 자리를 결제 진행"
     *
     * 대기열에서 승격(PAYMENT_PENDING)된 회원의 자리에 대해 
     * '결제 대기' 주문을 만들어 orderUid를 내려준다.
     * 정규 참여와 동일하게 프론트가 그 값으로 토스 결제창을 띄우고, 
     * 결제 성공 시 PARTICIPATING으로 확정된다.
     * 점유는 승격 시 이미 잡혀 있어 변동 없다.
     */
    @PostMapping("/{seq}/promotion-payment")
    public ResponseEntity<ParticipateResponse> startPromotedPayment(
            @PathVariable(name = "seq") Long seq,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(groupBuyService.startPromotedPayment(seq, user.getMemberSeq()));
    }

    /**
     * 결제대기 명시적 취소 (토스 결제창에서 사용자가 '취소'를 누른 경우).
     * 예) POST /api/group-buys/7/cancel-pending
     *
     * 토스는 사용자의 결제창 취소를 failUrl 리다이렉트가 아니라 
     * JS Promise reject로 알려주므로,
     * 프론트의 catch에서 이 API를 호출해 결제대기 자리를 
     * 즉시 반납(CANCELLED + 점유 해제)한다.
     * 결제 전이라 환불은 없다. 1인 1상품이라 optionSeq는 받지 않는다.
     */
    @PostMapping("/{seq}/cancel-pending")
    public ResponseEntity<Void> cancelPending(
            @PathVariable(name = "seq") Long seq,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        groupBuyService.cancelPendingPaymentByMember(seq, user.getMemberSeq());
        return ResponseEntity.ok().build();
    }

    /**
     * 업무 검증 실패(기본 배송지 없음, 이미 참여 중, 마감된 공구 등)를 400 + {message}로 변환한다.
     * 핸들러가 없으면 그대로 500이 나가 프론트가 원인을 구분하지 못하므로, 여기서 사용자에게
     * 보여줄 메시지(예: "기본 배송지가 없습니다. 배송지를 먼저 등록해주세요.")를 그대로 내려준다.
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBusinessError(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}

/*
	- ① @RestController:
	메서드가 돌려주는 객체를 자동으로 JSON으로 변환해서 내보냄 
	(변환은 Jackson이라는 라이브러리가 해줌)
	
	- ② @RequestMapping("/api/group-buys"): 
	이 컨트롤러 모든 메서드의 URL 앞부분
	그래서 아래 메서드들이 다 /api/group-buys로 시작
	
	- ③ @GetMapping (경로 없음): GET 요청 + ②의 경로 그대로 
	→ GET /api/group-buys = 목록.
	
	- ④ @GetMapping("/{seq}"): GET /api/group-buys/7 = 상세. 
	여기서 {seq}는 변하는 자리(변수)
	
	- @PathVariable Long seq: URL의 그 7을 꺼내서 seq 변수에 담음 
	그래서 getDetail(7)이 호출됨

*/