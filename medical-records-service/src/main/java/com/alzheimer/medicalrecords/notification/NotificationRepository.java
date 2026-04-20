package com.alzheimer.medicalrecords.notification;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n ORDER BY n.createdAt DESC")
    List<Notification> findAllOrderByCreatedAtDesc();

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.isRead = false")
    Long countAllUnread();

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
    void markAllReadForUser(@Param("userId") Long userId);
}
