package com.travolish.traveller.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "host_documents", indexes = {
    @Index(name = "idx_host_kyc_id", columnList = "host_kyc_id"),
    @Index(name = "idx_document_type", columnList = "document_type"),
    @Index(name = "idx_status", columnList = "verification_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_kyc_id", nullable = false)
    private HostKYC hostKYC;
    
    @Column(name = "document_type", nullable = false)
    private String documentType; // PASSPORT, ID, LICENSE, BUSINESS_LICENSE, TAX_CERTIFICATE, PROOF_OF_ADDRESS, BANK_STATEMENT
    
    @Column(name = "document_name")
    private String documentName;
    
    @Column(name = "file_url", nullable = false)
    private String fileUrl; // AWS S3 URL
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "file_type")
    private String fileType; // pdf, jpg, png, etc.
    
    @Column(name = "document_number")
    private String documentNumber; // e.g., passport number, ID number
    
    @Column(name = "issued_date")
    private java.time.LocalDate issuedDate;
    
    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;
    
    @Column(name = "issuing_country")
    private String issuingCountry;
    
    // Verification
    @Column(name = "verification_status", nullable = false)
    private String verificationStatus; // PENDING, VERIFIED, REJECTED, EXPIRED
    
    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;
    
    @Column(name = "verified_by")
    private String verifiedBy; // Admin email
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    // OCR/AI Verification
    @Column(name = "ai_verified")
    private Boolean aiVerified = false;
    
    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore; // 0.0 to 1.0
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
