package com.example.java.member.service;

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
}
