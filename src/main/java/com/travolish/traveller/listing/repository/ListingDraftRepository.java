package com.travolish.traveller.listing.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.listing.model.ListingDraft;
import com.travolish.traveller.listing.model.ListingDraft.DraftStatus;

public interface ListingDraftRepository extends JpaRepository<ListingDraft, Long> {

    List<ListingDraft> findByHostId(Long hostId);

    List<ListingDraft> findByHostIdAndStatus(Long hostId, DraftStatus status);

    Optional<ListingDraft> findByIdAndHostId(Long id, Long hostId);

    @Modifying
    @Transactional
    @Query("UPDATE ListingDraft d SET d.status = 'EXPIRED' WHERE d.expiresAt < :now AND d.status = 'IN_PROGRESS'")
    int expireOldDrafts(OffsetDateTime now);
}
