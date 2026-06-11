package com.example.java.member.constant;

/**
 * 회원 상태 코드 (member.status, member_status_log.prev/new_status 공통).
 *
 * DDL 코멘트 기준:
 * 1:활성 2:휴면 3:일시정지 4:탈퇴보류중 5:탈퇴
 */
public final class MemberStatus {

    private MemberStatus() {}

    /** 1: 활성(정상) */
    public static final int ACTIVE = 1;
    /** 2: 휴면 */
    public static final int DORMANT = 2;
    /** 3: 일시정지 */
    public static final int SUSPENDED = 3;
    /** 4: 탈퇴보류중 (탈퇴 신청 후 유예기간) */
    public static final int WITHDRAWAL_PENDING = 4;
    /** 5: 탈퇴(완료) */
    public static final int WITHDRAWN = 5;
}
