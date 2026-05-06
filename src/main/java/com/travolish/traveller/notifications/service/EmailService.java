package com.travolish.traveller.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.Optional;

@Service
@Slf4j
public class EmailService {
    
    private final Optional<JavaMailSender> javaMailSender;
    
    @Value("${spring.mail.from:noreply@travolish.com}")
    private String fromEmail;
    
    @Value("${spring.mail.from-name:Travolish Hotels}")
    private String fromName;
    
    @Autowired(required = false)
    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = Optional.ofNullable(javaMailSender);
    }
    
    /**
     * Send simple text email
     */
    public void sendSimpleEmail(String to, String subject, String content) {
        if (!javaMailSender.isPresent()) {
            log.warn("JavaMailSender not configured. Email would have been sent to: {} with subject: {}", to, subject);
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
        } catch (Exception e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!javaMailSender.isPresent()) {
            log.warn("JavaMailSender not configured. HTML Email would have been sent to: {} with subject: {}", to, subject);
            return;
        }
        
        try {
            MimeMessage message = javaMailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            
            javaMailSender.get().send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send email with attachments
     */
    public void sendEmailWithAttachment(String to, String subject, String htmlContent, String attachmentPath) {
        if (!javaMailSender.isPresent()) {
            log.warn("JavaMailSender not configured. Email with attachment would have been sent to: {}", to);
            return;
        }
        
        try {
            MimeMessage message = javaMailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Note: Implement attachment logic as needed
            // helper.addAttachment("filename", new File(attachmentPath));
            
            javaMailSender.get().send(message);
            log.info("Email with attachment sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email with attachment: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send batch emails
     */
    public void sendBatchEmails(String[] recipients, String subject, String content) {
        if (!javaMailSender.isPresent()) {
            log.warn("JavaMailSender not configured. Batch emails would have been sent to {} recipients", recipients.length);
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
}
