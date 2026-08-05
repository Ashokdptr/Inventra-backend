package com.inventra.notification;

import com.inventra.auth.User;
import com.inventra.auth.UserRepository;
import com.inventra.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getForUser(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadForUser(Long userId) {
        return notificationRepository.findUnreadByUserId(userId).stream()
                .map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> countUnread(Long userId) {
        return Map.of("unreadCount", notificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    public NotificationResponse markRead(Long id, Long userId) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!n.getUser().getId().equals(userId))
            throw new IllegalArgumentException("Access denied");
        n.setIsRead(true);
        return NotificationResponse.from(notificationRepository.save(n));
    }

    public void markAllRead(Long userId) {
        notificationRepository.findUnreadByUserId(userId).forEach(n -> n.setIsRead(true));
    }
    public void createInApp(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        notificationRepository.save(
                Notification.builder()
                        .user(user)
                        .type(Notification.Type.IN_APP)
                        .message(message)
                        .status(Notification.Status.SENT)
                        .isRead(false)
                        .build()
        );
    }

}
