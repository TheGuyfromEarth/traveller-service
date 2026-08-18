package com.travolish.traveller.notifications.repository;

import com.travolish.traveller.notifications.entity.NotificationTemplate;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    
    Optional<NotificationTemplate> findByType(NotificationType type);
    
    List<NotificationTemplate> findByChannel(NotificationChannel channel);
    
    List<NotificationTemplate> findByIsActiveTrue();
    
    List<NotificationTemplate> findByChannelAndIsActiveTrue(NotificationChannel channel);
    
    Optional<NotificationTemplate> findByTypeAndChannel(NotificationType type, NotificationChannel channel);
}
