package com.travolish.traveller.kyc.repository;

import com.travolish.traveller.kyc.entity.GuestKYC;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestKYCRepository extends JpaRepository<GuestKYC, Long> {

    @Query("SELECT g FROM GuestKYC g WHERE g.guestId = :guestId")
    Optional<GuestKYC> findByGuestId(@Param("guestId") Long guestId);

    @Query("SELECT g FROM GuestKYC g WHERE g.kycStatus = :status")
    List<GuestKYC> findByKycStatus(@Param("status") String status);

    @Query("SELECT g FROM GuestKYC g WHERE g.kycStatus = 'PENDING' ORDER BY g.createdAt ASC")
    List<GuestKYC> findPendingVerifications();

    long countByKycStatus(String kycStatus);
}
