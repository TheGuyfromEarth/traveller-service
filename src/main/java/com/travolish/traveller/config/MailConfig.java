package com.travolish.traveller.config;

import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Mail Configuration for SMTP
 * 
 * Properties required in application.yaml:
 * spring.mail.host: SMTP server host
 * spring.mail.port: SMTP server port
 * spring.mail.username: SMTP username
 * spring.mail.password: SMTP password
 * spring.mail.properties.mail.smtp.auth: true
 * spring.mail.properties.mail.smtp.starttls.enable: true
 */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean
    public JavaMailSender javaMailSender() {
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Set basic properties with defaults
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setUsername("your-email@gmail.com");
        mailSender.setPassword("your-app-password");
        
        // Configure mail properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        
        return mailSender;
    }
}
