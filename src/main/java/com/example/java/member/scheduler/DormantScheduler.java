package com.example.java.member.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.java.member.constant.MemberStatus;
import com.example.java.member.entity.Member;
import com.example.java.member.repository.MemberRepository;
import com.example.java.member.service.DormantService;
import com.example.java.member.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회원 휴면 처리 스케줄러.
 *
 * 사전 안내 — 매일 05:00. 마지막 접속(없으면 가입일)이 335일(=1년-30일) 경과한 1일 윈도우 대상에 안내 메일.
 * 자동 전환 — 매일 05:30. 마지막 접속이 365일 경과한 활성 회원을 휴면 전환(status 1→2).
 *
 * 각 전환은 DormantService.toDormant()에서 개별 트랜잭션으로 처리되어, 한 건 실패가 전체를 롤백하지 않는다.
 * (사전안내 중복발송 방지: 1일 윈도우 매칭 — 스케줄러가 매일 도는 것을 전제. 누락일엔 안내 1회 놓칠 수 있음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DormantScheduler {

    private final MemberRepository memberRepository;
    private final DormantService dormantService;
    private final EmailService emailService;

    /** 미접속 며칠 후 휴면 전환 */
    private static final int DORMANT_DAYS = 365;
    /** 휴면 전환 며칠 전에 사전 안내 */
    private static final int NOTICE_LEAD_DAYS = 30;

    @Scheduled(cron = "0 0 5 * * *")   // 매일 05:00
    public void sendDormantNotices() {
        LocalDate noticeDay = LocalDate.now().minusDays(DORMANT_DAYS - NOTICE_LEAD_DAYS); // 335일 전
        List<Member> targets = memberRepository.findDormantNoticeTargets(
                MemberStatus.ACTIVE,
                noticeDay.atStartOfDay(),
                noticeDay.plusDays(1).atStartOfDay());

        LocalDate scheduledDormant = LocalDate.now().plusDays(NOTICE_LEAD_DAYS); // 약 30일 후 전환 예정
        log.info("[휴면 사전안내 스케줄러] 대상: {}건", targets.size());

        for (Member member : targets) {
            try {
                emailService.sendDormantNoticeEmail(member.getEmail(), scheduledDormant);
            } catch (Exception e) {
                log.error("[휴면 사전안내 스케줄러] 실패 - memberSeq: {}, error: {}",
                        member.getSeq(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 30 5 * * *")   // 매일 05:30
    public void convertDormant() {
        LocalDate cutoffDate = LocalDate.now().minusDays(DORMANT_DAYS);
        List<Long> targets = memberRepository.findDormantTargetSeqs(
                MemberStatus.ACTIVE, cutoffDate.atStartOfDay());

        log.info("[휴면 전환 스케줄러] 대상: {}건", targets.size());

        int success = 0;
        for (Long memberSeq : targets) {
            try {
                dormantService.toDormant(memberSeq, true, DormantService.AUTO_REASON);
                success++;
            } catch (Exception e) {
                log.error("[휴면 전환 스케줄러] 실패 - memberSeq: {}, error: {}",
                        memberSeq, e.getMessage());
            }
        }
        log.info("[휴면 전환 스케줄러] 완료: {}/{}건", success, targets.size());
    }
}
