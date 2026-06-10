package com.example.java.groupbuy.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.groupbuy.dto.GroupBuyDetailResponse;
import com.example.java.groupbuy.dto.GroupBuySummaryResponse;
import com.example.java.groupbuy.dto.ParticipateResult;
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
     * 예) POST /api/group-buys/7/participate?optionSeq=3  → "7번 공구에 3번 옵션으로 참여 신청"
     *
     * 조회(GET)와 달리 서버 상태(점유·참여 기록·대기열)를 바꾸므로 POST를 쓴다.
     * 참여는 "누가" 하는지가 필수라, 로그인한 회원 정보를 함께 받는다.
     *
     * 선택 옵션이 매진이면 서비스가 대기열 등록으로 분기한다. 그래서 응답 본문에
     * 결과(PARTICIPATED/QUEUED)를 실어 보내, 화면이 "참여 완료" / "대기열 등록됨"을 구분하게 한다.
     */
    @PostMapping("/{seq}/participate")
    public ResponseEntity<ParticipateResult> participate(
            // @PathVariable: URL 경로의 {seq} 자리값을 꺼냄 (/group-buys/[7]/participate → seq=7)
            @PathVariable(name = "seq") Long seq,
            // @RequestParam: 쿼리스트링 ?optionSeq=3 의 값을 꺼냄 → 회원이 고른 옵션
            @RequestParam(name = "optionSeq") Long optionSeq,
            // @AuthenticationPrincipal: 지금 로그인한 사용자(SecurityContext의 principal)를 자동으로 주입.
            // 타입을 CustomUserDetails로 받았으니 거기 담아둔 getMemberSeq()를 바로 쓸 수 있다.
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            // 비로그인이면 principal이 없어 user가 null → 401(인증 필요)로 막는다.
            // (SecurityConfig가 현재 개발용 permitAll이라 비로그인도 들어올 수 있어 여기서 직접 방어)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 검증·점유·결제·참여기록·대기열은 서비스(트랜잭션)에 위임. 컨트롤러는 입력만 꺼내 넘긴다.
        ParticipateResult result = groupBuyService.participate(seq, optionSeq, user.getMemberSeq());
        // 결과를 JSON 본문으로 반환 → "PARTICIPATED" 또는 "QUEUED"
        return ResponseEntity.ok(result);
    }

    /**
     * 공구 참여 취소.
     * 예) POST /api/group-buys/7/cancel  → "7번 공구 내 참여를 취소"
     *
     * 1인 1상품이라 공구 + 로그인 회원이면 취소 대상 참여가 유일하게 특정되므로, optionSeq는 받지 않는다.
     * 취소 → 환불 → 점유 복구 → 같은 옵션 대기열 FIFO 승격까지 서비스가 한 트랜잭션으로 처리한다.
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
     * 승격자 결제.
     * 예) POST /api/group-buys/7/promotion-payment  → "7번 공구에서 승격된 내 자리를 결제로 확정"
     *
     * 대기열에서 승격(PAYMENT_PENDING)된 회원이 결제기한 내 직접 결제해 참여중(PARTICIPATING)으로 전환한다.
     * 점유는 승격 시 이미 잡혀 있어 변동 없고, 결제 완료로 확정 인원에 포함된다.
     */
    @PostMapping("/{seq}/promotion-payment")
    public ResponseEntity<Void> confirmPromotedPayment(
            @PathVariable(name = "seq") Long seq,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        groupBuyService.confirmPromotedPayment(seq, user.getMemberSeq());
        return ResponseEntity.ok().build();
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