package com.example.java.groupbuy.dto;

/**
 * 공구 참여 시도의 결과.
 *
 * participate()는 선택한 옵션의 매진 여부에 따라 두 갈래로 끝난다:
 *  - PARTICIPATED : 정원이 남아 정규 참여 성공 (점유 +1 → 결제 → participation INSERT)
 *  - QUEUED       : 옵션이 매진이라 대기열(waiting_queue)에 등록됨 (결제·점유 없음)
 *
 * 서비스가 void가 아니라 이 결과를 돌려주는 이유:
 * 호출하는 컨트롤러/화면이 "참여 완료"와 "대기열 등록됨"을 구분해
 * 서로 다른 안내 메시지를 보여줄 수 있어야 하기 때문이다.
 *
 * DB에 저장되는 엔티티 상태(ParticipationStatus 등, entity 패키지)와 달리
 * 계층 사이를 오가는 '동작 결과 값'이므로 dto 패키지에 둔다.
 */
public enum ParticipateResult {
    PARTICIPATED,
    QUEUED
}
