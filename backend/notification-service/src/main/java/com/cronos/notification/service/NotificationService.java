package com.cronos.notification.service;

import com.cronos.notification.entity.Notification;
import com.cronos.notification.repository.NotificationRepository;
import com.cronos.notification.security.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public record View(UUID id, UUID userId, String userEmail, String type, String subject, String body, String status,
                       String error, UUID orderId, LocalDateTime sentAt, LocalDateTime createdAt) {}

    private final NotificationRepository repository;
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String from;

    public NotificationService(NotificationRepository repository, ObjectProvider<JavaMailSender> mailSender,
                               @Value("${spring.mail.host:}") String mailHost,
                               @Value("${app.mail.from}") String from) {
        this.repository = repository;
        this.mailSender = mailSender.getIfAvailable();
        this.mailEnabled = this.mailSender != null && mailHost != null && !mailHost.isBlank();
        this.from = from;
        log.info(mailEnabled ? "E-mail delivery enabled via {}" : "E-mail delivery disabled (MAIL_HOST not set): notifications are recorded only", mailHost);
    }

    /** Always records the notification; the stored status says what really happened to the e-mail. */
    public View send(UUID userId, String userEmail, String type, String subject, String body, UUID orderId) {
        Notification.Type parsed;
        try {
            parsed = Notification.Type.valueOf(type);
        } catch (RuntimeException e) {
            throw ApiException.badRequest("Unknown notification type");
        }
        Notification notification = repository.save(new Notification(userId, userEmail, parsed, subject, body, orderId));
        if (!mailEnabled) {
            notification.setStatus(Notification.Status.SKIPPED);
        } else {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(userEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                notification.setStatus(Notification.Status.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (RuntimeException e) {
                String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                notification.setStatus(Notification.Status.FAILED);
                notification.setError(reason.length() > 500 ? reason.substring(0, 500) : reason);
                log.warn("E-mail for notification {} failed: {}", notification.getId(), reason);
            }
        }
        Notification saved = repository.save(notification);
        log.info("Notification {} for order {} -> {}", saved.getType(), saved.getOrderId(), saved.getStatus());
        return toView(saved);
    }

    public List<View> forUser(UUID userId) {
        return repository.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationService::toView).toList();
    }

    public Page<View> adminList(UUID orderId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending());
        return (orderId == null ? repository.findAll(pageable) : repository.findByOrderId(orderId, pageable)).map(NotificationService::toView);
    }

    private static View toView(Notification n) {
        return new View(n.getId(), n.getUserId(), n.getUserEmail(), n.getType().name(), n.getSubject(), n.getBody(),
                n.getStatus().name(), n.getError(), n.getOrderId(), n.getSentAt(), n.getCreatedAt());
    }
}
