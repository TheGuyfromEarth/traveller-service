package com.travolish.traveller.notifications.repository;

import com.travolish.traveller.notifications.entity.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {
    
    Optional<UserNotificationPreference> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
