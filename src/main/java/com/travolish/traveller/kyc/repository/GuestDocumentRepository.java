package com.travolish.traveller.kyc.repository;

import com.travolish.traveller.kyc.entity.GuestDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestDocumentRepository extends JpaRepository<GuestDocument, Long> {

    @Query("SELECT d FROM GuestDocument d WHERE d.guestKYC.id = :guestKycId")
    List<GuestDocument> findByGuestKYCId(@Param("guestKycId") Long guestKycId);

    @Query("SELECT d FROM GuestDocument d WHERE d.guestKYC.id = :guestKycId " +
           "AND d.documentType = :documentType ORDER BY d.createdAt DESC")
    List<GuestDocument> findAllByGuestKYCIdAndDocumentType(
        @Param("guestKycId") Long guestKycId,
        @Param("documentType") String documentType
    );

    @Query("SELECT COUNT(d) FROM GuestDocument d WHERE d.guestKYC.id = :guestKycId")
    Integer countByGuestKYCId(@Param("guestKycId") Long guestKycId);

    @Query("SELECT COUNT(d) FROM GuestDocument d WHERE d.guestKYC.id = :guestKycId " +
           "AND d.verificationStatus = 'VERIFIED'")
    Integer countVerifiedDocuments(@Param("guestKycId") Long guestKycId);
}
