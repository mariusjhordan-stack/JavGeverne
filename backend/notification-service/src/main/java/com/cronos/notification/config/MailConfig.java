package com.cronos.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host", matchIfMissing = false)
    public JavaMailSender javaMailSender() {
        log.info("Mail sender configured");
        return new JavaMailSenderImpl();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host", havingValue = "", matchIfMissing = true)
    public JavaMailSender noOpMailSender() {
        log.info("Mail sender not configured — notifications will be logged only");
        return new NoOpMailSender();
    }

    private static class NoOpMailSender extends JavaMailSenderImpl {
        @Override
        public void send(org.springframework.mail.SimpleMailMessage simpleMessage) {
            LoggerFactory.getLogger(NoOpMailSender.class)
                    .info("Simulated email to {}: {}",
                          simpleMessage.getTo(), simpleMessage.getSubject());
        }
    }
}
