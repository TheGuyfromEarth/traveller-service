package com.travolish.traveller.hosttools.repository;

import com.travolish.traveller.hosttools.entity.AutoReplyTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AutoReplyTemplateRepository extends JpaRepository<AutoReplyTemplate, Long> {
    
    List<AutoReplyTemplate> findByHostId(Long hostId);
    
    Page<AutoReplyTemplate> findByHostId(Long hostId, Pageable pageable);
    
    @Query("SELECT art FROM AutoReplyTemplate art WHERE art.hostId = :hostId AND art.isActive = true")
    List<AutoReplyTemplate> findActiveByHostId(Long hostId);
    
    @Query("SELECT art FROM AutoReplyTemplate art WHERE art.hostId = :hostId AND art.category = :category AND art.isActive = true")
    List<AutoReplyTemplate> findActiveByHostIdAndCategory(Long hostId, String category);
    
    @Query("SELECT art FROM AutoReplyTemplate art WHERE art.hostId = :hostId AND art.triggerKeyword = :keyword AND art.isActive = true")
    AutoReplyTemplate findByHostIdAndKeyword(Long hostId, String keyword);
    
    @Query("SELECT art FROM AutoReplyTemplate art WHERE art.hostId = :hostId ORDER BY art.displayOrder ASC")
    List<AutoReplyTemplate> findByHostIdOrderedByDisplay(Long hostId);
}
