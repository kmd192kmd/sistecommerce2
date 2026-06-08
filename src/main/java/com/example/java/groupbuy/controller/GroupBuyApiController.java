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
     * 공구 참여 (정규 참여).
     * 예) POST /api/group-buys/7/participate?optionSeq=3  → "7번 공구에 3번 옵션으로 참여"
     *
     * 조회(GET)와 달리 서버 상태(점유·참여 기록)를 바꾸므로 POST를 쓴다.
     * 참여는 "누가" 하는지가 필수라, 로그인한 회원 정보를 함께 받는다.
     */
    @PostMapping("/{seq}/participate")
    public ResponseEntity<Void> participate(
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
        // 검증·점유·결제·참여기록은 서비스(트랜잭션)에 위임. 컨트롤러는 입력만 꺼내 넘긴다.
        groupBuyService.participate(seq, optionSeq, user.getMemberSeq());
        // 본문 없이 200 OK만 반환 (성공 신호). 필요해지면 참여 결과 DTO로 바꿀 수 있다.
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