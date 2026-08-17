package com.travolish.traveller.notifications.service;

import com.travolish.traveller.notifications.entity.EmailLog;
import com.travolish.traveller.notifications.repository.EmailLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class EmailService {

    private final Optional<JavaMailSender> javaMailSender;

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Value("${spring.mail.from:noreply@travolish.com}")
    private String fromEmail;

    @Value("${spring.mail.from-name:Travolish}")
    private String fromName;

    @Autowired(required = false)
    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = Optional.ofNullable(javaMailSender);
    }

    /** Send simple text email */
    public void sendSimpleEmail(String to, String subject, String content) {
        if (!javaMailSender.isPresent()) {
            log.warn("JavaMailSender not configured. Email would have been sent to: {} with subject: {}", to, subject);
            persistLog(to, subject, content, "PLAIN", "SKIPPED", "SMTP not configured");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            javaMailSender.get().send(message);
            log.info("Simple email sent successfully to: {}", to);
            persistLog(to, subject, content, "PLAIN", "SENT", null);
        } catch (Exception e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage());
            persistLog(to, subject, content, "PLAIN", "FAILED", e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /** Send HTML email */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!javaMailSender.isPresent()) {
            log.warn("JavaMailSender not configured. HTML Email would have been sent to: {} with subject: {}", to, subject);
            persistLog(to, subject, htmlContent, "HTML", "SKIPPED", "SMTP not configured");
            return;
        }
        try {
            MimeMessage message = javaMailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            javaMailSender.get().send(message);
            log.info("HTML email sent successfully to: {}", to);
            persistLog(to, subject, htmlContent, "HTML", "SENT", null);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            persistLog(to, subject, htmlContent, "HTML", "FAILED", e.getMessage());
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }

    /** Send email with attachments */
    public void sendEmailWithAttachment(String to, String subject, String htmlContent, String attachmentPath) {
        if (!javaMailSender.isPresent()) {
            log.warn("JavaMailSender not configured. Email with attachment would have been sent to: {}", to);
            persistLog(to, subject, htmlContent, "ATTACHMENT", "SKIPPED", "SMTP not configured");
            return;
        }
        try {
            MimeMessage message = javaMailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            if (attachmentPath != null && !attachmentPath.isBlank()) {
                helper.addAttachment(new java.io.File(attachmentPath).getName(), new java.io.File(attachmentPath));
            }
            javaMailSender.get().send(message);
            log.info("Email with attachment sent successfully to: {}", to);
            persistLog(to, subject, htmlContent, "ATTACHMENT", "SENT", null);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
            persistLog(to, subject, htmlContent, "ATTACHMENT", "FAILED", e.getMessage());
            throw new RuntimeException("Failed to send email with attachment: " + e.getMessage(), e);
        }
    }

    /** Send batch emails */
    public void sendBatchEmails(String[] recipients, String subject, String content) {
        if (!javaMailSender.isPresent()) {
            log.warn("JavaMailSender not configured. Batch emails would have been sent to {} recipients", recipients.length);
            for (String recipient : recipients) {
                persistLog(recipient, subject, content, "BATCH", "SKIPPED", "SMTP not configured");
            }
            return;
        }
        try {
            for (String recipient : recipients) {
                sendSimpleEmail(recipient, subject, content);
            }
            log.info("Batch emails sent to {} recipients", recipients.length);
        } catch (Exception e) {
            log.error("Failed to send batch emails: {}", e.getMessage());
            throw new RuntimeException("Failed to send batch emails: " + e.getMessage(), e);
        }
    }

    private void persistLog(String recipient, String subject, String body,
                            String emailType, String status, String errorMessage) {
        try {
            EmailLog entry = new EmailLog();
            entry.setRecipient(recipient);
            entry.setSubject(subject != null ? subject : "");
            entry.setBody(body != null && body.length() > 5000 ? body.substring(0, 5000) + "…" : body);
            entry.setEmailType(emailType);
            entry.setStatus(status);
            entry.setErrorMessage(errorMessage);
            entry.setSentAt(LocalDateTime.now());
            emailLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to persist email log entry: {}", ex.getMessage());
        }
    }
}
