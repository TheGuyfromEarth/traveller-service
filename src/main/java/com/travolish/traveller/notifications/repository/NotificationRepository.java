package com.travolish.traveller.notifications.repository;

import com.travolish.traveller.notifications.entity.Notification;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationStatus;
import com.travolish.traveller.notifications.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndIsReadFalse(Long userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndType(Long userId, NotificationType type, Pageable pageable);
    
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
    
    Page<Notification> findByStatusAndRetryCountLessThan(NotificationStatus status, Integer maxRetries, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.status = 'SCHEDULED' AND n.scheduledTime <= :now")
    List<Notification> findScheduledNotificationsToSend(@Param("now") LocalDateTime now);
    
    List<Notification> findByBookingId(Long bookingId);
    
    List<Notification> findByHotelId(Long hotelId);
    
    List<Notification> findByTypeAndStatusOrderByCreatedAtDesc(NotificationType type, NotificationStatus status);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") Long userId);
    
    @Query("SELECT n FROM Notification n WHERE n.channel = :channel AND n.status = 'FAILED' AND n.retryCount < :maxRetries")
    List<Notification> findFailedNotificationsForRetry(@Param("channel") NotificationChannel channel, @Param("maxRetries") Integer maxRetries);
    
    @Query("SELECT DISTINCT n.userId FROM Notification n WHERE n.type = :type AND n.createdAt >= :since ORDER BY n.userId")
    List<Long> findUserIdsForNotificationType(@Param("type") NotificationType type, @Param("since") LocalDateTime since);

    // Fallback: find by recipient email for notifications where userId is not set
    @Query("SELECT n FROM Notification n WHERE n.recipientEmail = :email ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientEmail(@Param("email") String email);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientEmail = :email AND n.isRead = false")
    Long countUnreadByRecipientEmail(@Param("email") String email);
}
