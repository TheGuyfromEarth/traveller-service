package com.travolish.traveller.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "guest_documents", indexes = {
    @Index(name = "idx_guest_document_kyc_id", columnList = "guest_kyc_id"),
    @Index(name = "idx_guest_document_type", columnList = "document_type"),
    @Index(name = "idx_guest_document_status", columnList = "verification_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_kyc_id", nullable = false)
    private GuestKYC guestKYC;

    // Supported values: GOVERNMENT_ID, PROOF_OF_ADDRESS
    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "issuing_country")
    private String issuingCountry;

    // Verification
    @Column(name = "verification_status", nullable = false)
    private String verificationStatus; // PENDING, VERIFIED, REJECTED, EXPIRED

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
