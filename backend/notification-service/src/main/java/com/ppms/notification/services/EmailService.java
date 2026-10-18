package com.ppms.notification.services;

import com.ppms.notification.domain.EmailOutbox;
import com.ppms.notification.repositories.EmailOutboxRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailOutboxRepository outboxRepository;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${ppms.mail.from-name:PPMS}")
    private String fromName;

    @Transactional
    public void enqueue(String to, String subject, String templateName, Context context) {
        String body = templateEngine.process("mail/" + templateName, context);
        EmailOutbox email = new EmailOutbox();
        email.setId(java.util.UUID.randomUUID());
        email.setRecipient(to);
        email.setSubject(subject);
        email.setBodyHtml(body);
        email.setStatus(EmailOutbox.Status.PENDING);
        email.setCreatedAt(Instant.now());
        email.setUpdatedAt(Instant.now());
        outboxRepository.save(email);
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processOutbox() {
        outboxRepository.findByStatus(EmailOutbox.Status.PENDING).forEach(email -> {
            try {
                send(email);
                email.setStatus(EmailOutbox.Status.SENT);
                email.setSentAt(Instant.now());
            } catch (Exception e) {
                log.error("Failed to send email to {}", email.getRecipient(), e);
                email.setAttempts(email.getAttempts() + 1);
                if (email.getAttempts() >= 3) {
                    email.setStatus(EmailOutbox.Status.FAILED);
                }
                email.setError(e.getMessage());
            }
            email.setUpdatedAt(Instant.now());
            outboxRepository.save(email);
        });
    }

    private void send(EmailOutbox email) throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail, fromName);
        helper.setTo(email.getRecipient());
        helper.setSubject(email.getSubject());
        helper.setText(email.getBodyHtml(), true);
        mailSender.send(message);
    }
}
