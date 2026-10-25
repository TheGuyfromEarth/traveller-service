package com.travolish.traveller.chat.repository;

import com.travolish.traveller.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.userId1 = :userId1 AND c.userId2 = :userId2) OR " +
           "(c.userId1 = :userId2 AND c.userId2 = :userId1)")
    Optional<Conversation> findConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    @Query("SELECT c FROM Conversation c WHERE " +
           "(c.userId1 = :userId AND c.isActive = true) OR " +
           "(c.userId2 = :userId AND c.isActive = true) " +
           "ORDER BY c.lastMessageTime DESC")
    List<Conversation> findUserConversations(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Conversation c WHERE c.id IN " +
           "(SELECT c2.id FROM Conversation c2 WHERE " +
           "(c2.userId1 = :userId AND c2.user1UnreadCount > 0) OR " +
           "(c2.userId2 = :userId AND c2.user2UnreadCount > 0))")
    List<Conversation> findConversationsWithUnreadMessages(@Param("userId") Long userId);
}
