package com.example.java.member.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.java.member.constant.MemberStatus;
import com.example.java.member.entity.Member;
import com.example.java.member.entity.MemberStatusLog;
import com.example.java.member.entity.MemberWithdrawal;
import com.example.java.member.entity.WithdrawalReason;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.repository.MemberStatusLogRepository;
import com.example.java.member.repository.MemberWithdrawalRepository;
import com.example.java.member.repository.WithdrawalReasonRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 탈퇴 신청·복구 서비스.
 *
 * - 신청: status 1→4(탈퇴보류중), member 원본을 member_withdrawal 에 분리보관, 3일 유예, 자동로그인 토큰 무효화.
 * - 복구: 유예기간 내 status 4→1(활성), 분리보관 레코드 삭제.
 *
 * 실제 마스킹(member 변경)은 유예 경과 후 확정 스케줄러에서 수행한다(여기서는 member 데이터 보존).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final MemberRepository memberRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final MemberWithdrawalRepository memberWithdrawalRepository;
    private final MemberStatusLogRepository memberStatusLogRepository;
    private final RememberMeTokenService rememberMeTokenService;

    /** 탈퇴 신청 후 복구 가능 유예일수 */
    private static final int GRACE_DAYS = 3;

    /**
     * 탈퇴 신청.
     * 활성(status=1) 회원만 가능. member 원본을 분리보관하고 status 를 4로 전환한다.
     */
    @Transactional
    public void request(Long memberSeq, Long reasonSeq) {
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.getStatus() == null || member.getStatus() != MemberStatus.ACTIVE) {
            throw new IllegalStateException("탈퇴 신청할 수 없는 상태입니다.");
        }

        WithdrawalReason reason = withdrawalReasonRepository.findById(reasonSeq)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 탈퇴 사유입니다."));

        int prevStatus = member.getStatus();

        // 1) 마스킹 전 원본 스냅샷 분리보관 (유예 만료일 = 신청+3일)
        LocalDate scheduledDeleteAt = LocalDate.now().plusDays(GRACE_DAYS);
        memberWithdrawalRepository.save(MemberWithdrawal.request(member, reason, scheduledDeleteAt));

        // 2) 상태 전환 1→4
        member.markWithdrawalRequested();
        memberStatusLogRepository.save(
                MemberStatusLog.system(member, prevStatus, MemberStatus.WITHDRAWAL_PENDING, "회원 탈퇴 신청"));

        // 3) 자동로그인 토큰 전체 무효화
        rememberMeTokenService.invalidateAll(member.getSeq());

        log.info("탈퇴 신청 - memberSeq: {}, reasonSeq: {}, 유예만료: {}", memberSeq, reasonSeq, scheduledDeleteAt);
    }

    /**
     * 탈퇴 복구.
     * 탈퇴보류중(status=4) 회원만 가능. status 를 1로 되돌리고 분리보관 레코드를 삭제한다.
     */
    @Transactional
    public void restore(Long memberSeq) {
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (member.getStatus() == null || member.getStatus() != MemberStatus.WITHDRAWAL_PENDING) {
            throw new IllegalStateException("복구할 수 있는 상태가 아닙니다.");
        }

        MemberWithdrawal withdrawal = memberWithdrawalRepository
                .findFirstByMember_SeqAndWithdrawalYnOrderBySeqDesc(memberSeq, MemberWithdrawal.YN_N)
                .orElseThrow(() -> new IllegalStateException("진행 중인 탈퇴 신청이 없습니다."));

        int prevStatus = member.getStatus();

        // 1) 상태 전환 4→1
        member.restoreActive();
        memberStatusLogRepository.save(
                MemberStatusLog.system(member, prevStatus, MemberStatus.ACTIVE, "회원 탈퇴 복구"));

        // 2) 분리보관 레코드 삭제 (복구되었으므로 보관 불필요, member 원본은 마스킹 전이라 그대로 유지됨)
        memberWithdrawalRepository.delete(withdrawal);

        log.info("탈퇴 복구 - memberSeq: {}", memberSeq);
    }

    /** 탈퇴 화면 표시용 조회: 현재 상태 + (보류중이면) 유예 만료일 + 사유 목록 */
    @Transactional(readOnly = true)
    public WithdrawalView view(Long memberSeq) {
        Member member = memberRepository.findById(memberSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        List<WithdrawalReason> reasons = withdrawalReasonRepository.findAllByOrderBySeqAsc();

        LocalDate scheduledDeleteAt = null;
        if (member.getStatus() != null && member.getStatus() == MemberStatus.WITHDRAWAL_PENDING) {
            scheduledDeleteAt = memberWithdrawalRepository
                    .findFirstByMember_SeqAndWithdrawalYnOrderBySeqDesc(memberSeq, MemberWithdrawal.YN_N)
                    .map(MemberWithdrawal::getScheduledDeleteAt)
                    .orElse(null);
        }

        return new WithdrawalView(member.getStatus(), scheduledDeleteAt, reasons);
    }

    /** 탈퇴 화면 모델 */
    public record WithdrawalView(Integer status, LocalDate scheduledDeleteAt, List<WithdrawalReason> reasons) {
        public boolean isPending() {
            return status != null && status == MemberStatus.WITHDRAWAL_PENDING;
        }
        public boolean isActive() {
            return status != null && status == MemberStatus.ACTIVE;
        }
    }
}
