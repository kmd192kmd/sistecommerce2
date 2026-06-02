package com.example.java.groupbuy.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.java.groupbuy.dto.GroupBuyDetailResponse;
import com.example.java.groupbuy.dto.GroupBuySummaryResponse;
import com.example.java.groupbuy.service.GroupBuyService;

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
    public GroupBuyDetailResponse detail(@PathVariable Long seq) {
        return groupBuyService.getDetail(seq);
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