package com.example.java.member.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.constant.MemberStatus;
import com.example.java.member.entity.Member;
import com.example.java.member.entity.MemberDormant;
import com.example.java.member.entity.MemberStatusLog;
import com.example.java.member.repository.MemberDormantRepository;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.repository.MemberStatusLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 휴면 전환·복원 서비스.
 *
 * - 전환: member 민감정보를 member_dormant 로 평문 분리보관 + member 민감 컬럼 null, status 1→2.
 *         자동로그인 토큰 무효화, member_status_log(1→2).
 * - 복원: member_dormant 스냅샷으로 member 복구, status 2→1, 분리보관 레코드 삭제, member_status_log(2→1).
 *
 * (암호화는 member_dormant 컬럼 크기 제약으로 미적용 — 평문 분리보관 방식)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DormantService {

    private final MemberRepository memberRepository;
    private final MemberDormantRepository memberDormantRepository;
    private final MemberStatusLogRepository memberStatusLogRepository;
    private final RememberMeTokenService rememberMeTokenService;

    /** 1년 이상 미접속 자동 휴면 사유 */
    public static final String AUTO_REASON = "1년 이상 미접속 자동 휴면";
    /** 휴면 상태 보관 만료(삭제) 예정까지 연수 (placeholder — 정책 확정 시 조정) */
    private static final int DORMANT_RETENTION_YEARS = 5;

    /**
     * 휴면 전환(1건). 활성(status=1) 회원만 처리한다.
     *
     * @param auto   true=스케줄러 자동, false=관리자 수동
     * @param reason 휴면 사유
     */
    @Transactional
    public void toDormant(Long memberSeq, boolean auto, String reason) {
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.getStatus() == null || member.getStatus() != MemberStatus.ACTIVE) {
            return; // 이미 휴면/탈퇴 등 → 스킵
        }

        int prevStatus = member.getStatus();

        // 1) 마스킹 전 원본 스냅샷 분리보관 (member 의 현재 값 복사)
        LocalDate scheduledDeleteAt = LocalDate.now().plusYears(DORMANT_RETENTION_YEARS);
        memberDormantRepository.save(MemberDormant.of(member, reason, auto, scheduledDeleteAt));

        // 2) member 민감정보 제거 + status 1→2
        member.enterDormant();
        memberStatusLogRepository.save(
                MemberStatusLog.system(member, prevStatus, MemberStatus.DORMANT, reason));

        // 3) 자동로그인 토큰 전체 무효화
        rememberMeTokenService.invalidateAll(member.getSeq());

        log.info("휴면 전환 - memberSeq: {}, auto: {}", memberSeq, auto);
    }

    /**
     * 휴면 복원(1건). 휴면(status=2) 회원만 처리한다.
     * member_dormant 스냅샷으로 member 를 복구하고 status 를 1로 되돌린 뒤 분리보관 레코드를 삭제한다.
     */
    @Transactional
    public void restore(Long memberSeq) {
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.getStatus() == null || member.getStatus() != MemberStatus.DORMANT) {
            return; // 휴면 상태가 아니면 스킵
        }

        MemberDormant dormant = memberDormantRepository
                .findFirstByMember_SeqOrderBySeqDesc(memberSeq)
                .orElseThrow(() -> new IllegalStateException("휴면 분리보관 레코드가 없습니다."));

        int prevStatus = member.getStatus();

        member.restoreFromDormant(dormant);
        memberStatusLogRepository.save(
                MemberStatusLog.system(member, prevStatus, MemberStatus.ACTIVE, "휴면 복원"));

        memberDormantRepository.delete(dormant);

        log.info("휴면 복원 - memberSeq: {}", memberSeq);
    }
}
