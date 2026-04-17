package com.alzheimer.medicalrecords.notification;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;

import com.alzheimer.medicalrecords.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/all")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Notification>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("All notifications",
                notificationRepository.findAllOrderByCreatedAtDesc()));
    }

    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<Notification>>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("User notifications",
                notificationRepository.findByUserId(userId)));
    }

    @GetMapping("/unread-count")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success("Unread count",
                notificationRepository.countAllUnread()));
    }

    @PatchMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @PatchMapping("/mark-all-read")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markAllRead(@RequestBody Map<String, Object> body) {
        if (body.get("userId") != null) {
            notificationRepository.markAllReadForUser(Long.valueOf(body.get("userId").toString()));
        }
        return ResponseEntity.ok(ApiResponse.success("All marked as read", null));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }
}
