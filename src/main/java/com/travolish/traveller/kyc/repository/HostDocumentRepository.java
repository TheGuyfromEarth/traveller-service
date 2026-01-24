package com.travolish.traveller.kyc.repository;

import com.travolish.traveller.kyc.entity.HostDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HostDocumentRepository extends JpaRepository<HostDocument, Long> {
    
    @Query("SELECT hd FROM HostDocument hd WHERE hd.hostKYC.id = :hostKYCId")
    List<HostDocument> findByHostKYCId(@Param("hostKYCId") Long hostKYCId);
    
    @Query("SELECT hd FROM HostDocument hd WHERE hd.hostKYC.id = :hostKYCId " +
           "AND hd.documentType = :documentType")
    Optional<HostDocument> findByHostKYCIdAndDocumentType(
        @Param("hostKYCId") Long hostKYCId,
        @Param("documentType") String documentType
    );
    
    @Query("SELECT hd FROM HostDocument hd WHERE hd.hostKYC.id = :hostKYCId " +
           "AND hd.verificationStatus = :status")
    List<HostDocument> findByHostKYCIdAndVerificationStatus(
        @Param("hostKYCId") Long hostKYCId,
        @Param("status") String status
    );
    
    @Query("SELECT COUNT(hd) FROM HostDocument hd WHERE hd.hostKYC.id = :hostKYCId " +
           "AND hd.verificationStatus = 'VERIFIED'")
    Integer countVerifiedDocuments(@Param("hostKYCId") Long hostKYCId);
    
    @Query("SELECT hd FROM HostDocument hd WHERE hd.verificationStatus = 'PENDING' " +
           "ORDER BY hd.createdAt ASC")
    List<HostDocument> findPendingDocuments();
    
    @Query("SELECT hd FROM HostDocument hd WHERE hd.aiVerified = true " +
           "AND hd.aiConfidenceScore >= :confidenceThreshold")
    List<HostDocument> findAIVerifiedDocuments(@Param("confidenceThreshold") Double confidenceThreshold);
    
    @Query("SELECT COUNT(hd) FROM HostDocument hd WHERE hd.hostKYC.id = :hostKYCId")
    Integer countByHostKYCId(@Param("hostKYCId") Long hostKYCId);
}
