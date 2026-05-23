package com.hometalk.onepass.auth.service;

import jakarta.servlet.http.HttpSession;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String EMAIL_ATTR = "signupEmailVerificationEmail";
    private static final String CODE_ATTR = "signupEmailVerificationCode";
    private static final String EXPIRES_AT_ATTR = "signupEmailVerificationExpiresAt";
    private static final String VERIFIED_ATTR = "signupEmailVerificationVerified";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.from:}")
    private String from;

    public void sendCode(String email, HttpSession session) {
        String normalizedEmail = normalizeEmail(email);
        Object verified = session.getAttribute(VERIFIED_ATTR);
        Object expiresAt = session.getAttribute(EXPIRES_AT_ATTR);
        if (!Boolean.TRUE.equals(verified)
                && expiresAt instanceof Instant expires
                && Instant.now().isBefore(expires)) {
            long remainingSeconds = Duration.between(Instant.now(), expires).toSeconds();
            throw new IllegalArgumentException("인증 메일은 5분에 한 번만 발송할 수 있습니다. "
                    + Math.max(1, remainingSeconds) + "초 후 다시 시도해 주세요.");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        session.setAttribute(EMAIL_ATTR, normalizedEmail);
        session.setAttribute(CODE_ATTR, code);
        session.setAttribute(EXPIRES_AT_ATTR, Instant.now().plus(CODE_TTL));
        session.setAttribute(VERIFIED_ATTR, false);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            if (from != null && !from.isBlank()) {
                helper.setFrom(from);
            }
            helper.setTo(normalizedEmail);
            helper.setSubject("[Home Talk One Pass] 이메일 인증 코드");
            helper.setText("""
                    <div style="font-family: Arial, sans-serif; color: #222; line-height: 1.6;">
                        <p>Home Talk One Pass 회원가입 이메일 인증 코드입니다.</p>
                        <p>본 메일은 5분동안 유효합니다.</p>
                        <p style="margin-top: 20px;">인증 코드</p>
                        <p style="margin: 0; color: #4073C9; font-size: 28px; font-weight: 700; letter-spacing: 3px;">%s</p>
                    </div>
                    """.formatted(code), true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            clear(session);
            throw new IllegalStateException("인증 메일 발송에 실패했습니다. 메일 설정을 확인해 주세요.", e);
        }
    }

    public void verifyCode(String email, String code, HttpSession session) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedCode = normalizeCode(code);

        Object savedEmail = session.getAttribute(EMAIL_ATTR);
        Object savedCode = session.getAttribute(CODE_ATTR);
        Object expiresAt = session.getAttribute(EXPIRES_AT_ATTR);

        if (!(expiresAt instanceof Instant expires) || Instant.now().isAfter(expires)) {
            clear(session);
            throw new IllegalArgumentException("인증 코드가 만료되었습니다. 다시 발송해 주세요.");
        }

        if (!normalizedEmail.equals(savedEmail) || !normalizedCode.equals(savedCode)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
        }

        session.setAttribute(VERIFIED_ATTR, true);
    }

    public void assertVerified(String email, HttpSession session) {
        String normalizedEmail = normalizeEmail(email);
        Object savedEmail = session.getAttribute(EMAIL_ATTR);
        Object verified = session.getAttribute(VERIFIED_ATTR);

        if (!normalizedEmail.equals(savedEmail) || !Boolean.TRUE.equals(verified)) {
            throw new IllegalArgumentException("이메일 인증을 완료해 주세요.");
        }
    }

    public void clear(HttpSession session) {
        session.removeAttribute(EMAIL_ATTR);
        session.removeAttribute(CODE_ATTR);
        session.removeAttribute(EXPIRES_AT_ATTR);
        session.removeAttribute(VERIFIED_ATTR);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해 주세요.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (!normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }

        return normalizedEmail;
    }

    private String normalizeCode(String code) {
        if (code == null || !code.trim().matches("^\\d{6}$")) {
            throw new IllegalArgumentException("6자리 인증 코드를 입력해 주세요.");
        }

        return code.trim();
    }
}
