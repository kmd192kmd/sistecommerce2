package com.example.java.groupbuy.entity;

/**
 * 공동구매(group_buy) 진행 상태.
 * DB에는 @Enumerated(STRING)으로 enum 이름 그대로 저장된다. (DDL: status varchar2(20))
 */
public enum GroupBuyStatus {
    SCHEDULED,  // 시작 전 (등록되어 시작 대기)
    ONGOING,    // 진행 중 (참여 모집)
    CONFIRMED,  // 확정 마감 (최소 인원 달성)
    FAILED,     // 무산 (최소 인원 미달, 전원 환불)
    STOPPED     // 관리자 강제 중단
}

/*
	정해진 값만 가질 수 있는 상태에 enum을 쓰는 이유
	1. 오타를 컴파일 단계에서 막아줌
		- 만약 status를 그냥 String으로 썼다면
		ONGOIN <- 이렇게 오타났을 때 컴파일은 통과
		-> 런타임에서 조용히 에러 발생함, 원인 찾기 힘듦
			
	2. 숫자 매직값을 쓰지 않아도 된다
		- 만약 status를 int(0,1,2,3,4)로 썼다면 
		if (status == 1) 이게 무슨 뜻인지 
		DDL 주석을 매번 찾아봐야 함
*/
