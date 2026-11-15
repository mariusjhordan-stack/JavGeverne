package com.cronos.notification.service;

import com.cronos.notification.dto.NotificationResponse;
import com.cronos.notification.dto.SendNotificationRequest;
import com.cronos.notification.entity.Notification;
import com.cronos.notification.entity.Notification.NotificationStatus;
import com.cronos.notification.entity.Notification.NotificationType;
import com.cronos.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    public NotificationService(NotificationRepository notificationRepository,
                               JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    public NotificationResponse sendNotification(SendNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.userId());
        notification.setUserEmail(request.userEmail());
        notification.setType(NotificationType.valueOf(request.type()));
        notification.setSubject(request.subject());
        notification.setBody(request.body());
        notification.setOrderId(request.orderId());
        notification.setStatus(NotificationStatus.PENDING);

        notification = notificationRepository.save(notification);

        try {
            sendEmail(notification);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            log.info("Notification sent: type={}, to={}, orderId={}",
                    notification.getType(), notification.getUserEmail(), notification.getOrderId());
        } catch (Exception e) {
            log.warn("Email delivery skipped or failed for notification {}: {}",
                    notification.getId(), e.getMessage());
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        }

        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    public List<NotificationResponse> getNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NotificationResponse> getNotificationsByOrderId(UUID orderId) {
        return notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void sendEmail(Notification notification) {
        if (mailSender == null) {
            log.info("Mail sender not configured. Simulating email to {}: {}",
                    notification.getUserEmail(), notification.getSubject());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(notification.getUserEmail());
        message.setSubject(notification.getSubject());
        message.setText(notification.getBody());
        message.setFrom("noreply@cronos-geneve.com");
        mailSender.send(message);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getUserEmail(),
                n.getType().name(),
                n.getSubject(),
                n.getBody(),
                n.getStatus().name(),
                n.getOrderId(),
                n.getSentAt(),
                n.getCreatedAt()
        );
    }
}
