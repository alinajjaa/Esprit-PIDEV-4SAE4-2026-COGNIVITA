package com.alzheimer.notification.repository;

import com.alzheimer.notification.entity.NotificationChannel;
import com.alzheimer.notification.entity.NotificationRecord;
import com.alzheimer.notification.entity.NotificationStatus;
import com.alzheimer.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationRecord, Long> {

    List<NotificationRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationRecord> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status);

    @Query("SELECT n FROM NotificationRecord n WHERE n.userId = :userId AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<NotificationRecord> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * FIX: was countByUserIdAndStatus(userId, SENT) which included email and SMS records,
     * inflating the in-app badge count. Now filters by both channel and status.
     */
    long countByUserIdAndChannelAndStatus(Long userId, NotificationChannel channel, NotificationStatus status);

    /**
     * Used by mark-all-as-read endpoint to bulk update IN_APP SENT → READ.
     */
    List<NotificationRecord> findByUserIdAndChannelAndStatus(
            Long userId, NotificationChannel channel, NotificationStatus status);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    List<NotificationRecord> findByNotificationTypeAndCreatedAtAfter(NotificationType type, LocalDateTime after);

    @Query("SELECT n FROM NotificationRecord n WHERE n.escalationGroupId = :groupId ORDER BY n.escalationLevel ASC")
    List<NotificationRecord> findByEscalationGroupId(@Param("groupId") String groupId);
}
