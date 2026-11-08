package com.travolish.traveller.kyc.repository;

import com.travolish.traveller.kyc.entity.HostKYC;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HostKYCRepository extends JpaRepository<HostKYC, Long> {
    
    @Query("SELECT hk FROM HostKYC hk WHERE hk.hostId = :hostId")
    Optional<HostKYC> findByHostId(@Param("hostId") Long hostId);
    
    @Query("SELECT hk FROM HostKYC hk WHERE hk.kycStatus = :status")
    List<HostKYC> findByKYCStatus(@Param("status") String status);
    
    @Query("SELECT hk FROM HostKYC hk WHERE hk.verificationLevel = :level")
    List<HostKYC> findByVerificationLevel(@Param("level") String level);
    
    @Query("SELECT hk FROM HostKYC hk WHERE hk.riskLevel = :riskLevel " +
           "ORDER BY hk.riskScore DESC")
    List<HostKYC> findByRiskLevel(@Param("riskLevel") String riskLevel);
    
    @Query("SELECT hk FROM HostKYC hk WHERE hk.kycStatus = 'PENDING' " +
           "ORDER BY hk.createdAt ASC")
    List<HostKYC> findPendingVerifications();
    
    @Query("SELECT COUNT(hk) FROM HostKYC hk WHERE hk.kycStatus = 'VERIFIED'")
    Integer countVerifiedHosts();

    long countByKycStatus(String kycStatus);

    @Query("SELECT k FROM HostKYC k WHERE k.kycStatus IN :statuses AND k.updatedAt IS NOT NULL ORDER BY k.updatedAt DESC")
    List<HostKYC> findRecentByStatuses(@Param("statuses") List<String> statuses, Pageable pageable);
}
