package com.travolish.traveller.user.repository;

import com.travolish.traveller.user.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    /** All contacts for a user — primary first, then by creation date ascending. */
    List<EmergencyContact> findByUserIdOrderByIsPrimaryDescCreatedAtAsc(Long userId);

    /** Used by the controller to enforce per-user limits. */
    long countByUserId(Long userId);

    /** Ownership check before updates or deletes. */
    boolean existsByIdAndUserId(Long id, Long userId);

    /** Scoped delete — prevents one user from deleting another user's contact. */
    void deleteByIdAndUserId(Long id, Long userId);

    /** Finds the current primary contact for a user (may be empty). */
    Optional<EmergencyContact> findFirstByUserIdAndIsPrimaryTrue(Long userId);
}
