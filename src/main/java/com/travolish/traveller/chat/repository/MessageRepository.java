package com.travolish.traveller.chat.repository;

import com.travolish.traveller.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.isDeleted = false " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findByConversationIdPaged(@Param("conversationId") Long conversationId, Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.isDeleted = false " +
           "ORDER BY m.createdAt ASC")
    List<Message> findByConversationId(@Param("conversationId") Long conversationId);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.isRead = false " +
           "AND m.receiverId = :userId AND m.isDeleted = false " +
           "ORDER BY m.createdAt ASC")
    List<Message> findUnreadMessages(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.isRead = false AND m.receiverId = :userId AND m.isDeleted = false")
    Integer countUnreadMessages(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
