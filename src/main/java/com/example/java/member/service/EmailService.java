package com.example.java.member.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = baseUrl + "/member/reset-password?token=" + token;

        String subject = "[Gold Market] 비밀번호 재설정 안내";
        String content = """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px; border: 1px solid #e2e8f0; border-radius: 12px;">
                  <h2 style="color: #1e293b; margin-bottom: 8px;">비밀번호 재설정</h2>
                  <p style="color: #64748b; font-size: 14px; margin-bottom: 24px;">
                    아래 버튼을 클릭하면 새 비밀번호를 설정할 수 있습니다.<br>
                    링크는 <strong>1시간</strong> 동안 유효하며, 1회만 사용 가능합니다.
                  </p>
                  <a href="%s"
                     style="display: inline-block; background: #f59e0b; color: #1e293b; font-weight: bold;
                            text-decoration: none; padding: 12px 28px; border-radius: 8px; font-size: 15px;">
                    비밀번호 재설정하기
                  </a>
                  <p style="color: #94a3b8; font-size: 12px; margin-top: 24px;">
                    본인이 요청하지 않은 경우 이 메일을 무시하세요.
                  </p>
                </div>
                """.formatted(resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("비밀번호 재설정 메일 발송 실패: to={}", toEmail, e);
            throw new RuntimeException("메일 발송에 실패했습니다.");
        }
    }

    /**
     * 휴면 전환 사전 안내 메일.
     * 발송 실패해도 예외를 던지지 않고 로그만 남긴다(스케줄러 일괄 처리 중단 방지).
     *
     * @param toEmail          수신 이메일
     * @param scheduledDormant 휴면 전환 예정일
     */
    public void sendDormantNoticeEmail(String toEmail, LocalDate scheduledDormant) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }

        String subject = "[Gold Market] 장기 미접속 휴면 전환 예정 안내";
        String content = """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px; border: 1px solid #e2e8f0; border-radius: 12px;">
                  <h2 style="color: #1e293b; margin-bottom: 8px;">휴면 전환 예정 안내</h2>
                  <p style="color: #64748b; font-size: 14px; margin-bottom: 24px;">
                    장기간 접속하지 않아 회원님의 계정이 <strong>%s</strong>에 휴면 상태로 전환될 예정입니다.<br>
                    휴면 전환 시 일부 개인정보는 별도로 분리 보관되며, 다시 로그인하시면 본인 확인 후 정상 복원됩니다.
                  </p>
                  <a href="%s/member/login"
                     style="display: inline-block; background: #f59e0b; color: #1e293b; font-weight: bold;
                            text-decoration: none; padding: 12px 28px; border-radius: 8px; font-size: 15px;">
                    지금 로그인하여 휴면 전환 방지
                  </a>
                  <p style="color: #94a3b8; font-size: 12px; margin-top: 24px;">
                    휴면 전환을 원하지 않으시면 예정일 이전에 로그인해 주세요.
                  </p>
                </div>
                """.formatted(scheduledDormant, baseUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("휴면 사전 안내 메일 발송 실패: to={}", toEmail, e);
        }
    }

    /**
     * 회원가입 이메일 인증번호 메일.
     * 발송 실패 시 예외를 던진다(호출 측에서 발송 실패를 사용자에게 알리도록).
     */
    public void sendVerificationCodeEmail(String toEmail, String code) {
        String subject = "[Gold Market] 회원가입 이메일 인증번호";
        String content = """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px; border: 1px solid #e2e8f0; border-radius: 12px;">
                  <h2 style="color: #1e293b; margin-bottom: 8px;">이메일 인증번호</h2>
                  <p style="color: #64748b; font-size: 14px; margin-bottom: 24px;">
                    아래 인증번호를 회원가입 화면에 입력해 주세요.<br>
                    인증번호는 <strong>5분간</strong> 유효합니다.
                  </p>
                  <div style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #f59e0b;
                              background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px;
                              text-align: center; padding: 16px 0;">%s</div>
                  <p style="color: #94a3b8; font-size: 12px; margin-top: 24px;">
                    본인이 요청하지 않은 경우 이 메일을 무시하세요.
                  </p>
                </div>
                """.formatted(code);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("인증번호 메일 발송 실패: to={}", toEmail, e);
            throw new RuntimeException("인증 메일 발송에 실패했습니다.");
        }
    }
}
