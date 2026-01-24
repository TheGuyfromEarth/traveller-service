package com.travolish.traveller.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_user1", columnList = "user_id_1"),
    @Index(name = "idx_user2", columnList = "user_id_2"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id_1", nullable = false)
    private Long userId1;
    
    @Column(name = "user_id_2", nullable = false)
    private Long userId2;
    
    @Column(name = "last_message_id")
    private Long lastMessageId;
    
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Message> messages = new ArrayList<>();
    
    @Column(name = "user1_unread_count", nullable = false)
    private Integer user1UnreadCount = 0;
    
    @Column(name = "user2_unread_count", nullable = false)
    private Integer user2UnreadCount = 0;
}
