package com.travolish.traveller.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "listing_descriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hotelId;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private String sourceLanguage;

    @Column(nullable = false)
    private String targetLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DescriptionType descriptionType;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String originalDescription;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String generatedDescription;

    @Column(nullable = false)
    private Double qualityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenerationStatus status;

    @Column(nullable = false)
    private Boolean approved;

    private Boolean isActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime approvedAt;

    private String approvalNotes;

    private String aiModel;

    private Double confidenceScore;

    private Integer characterCount;

    private Integer wordCount;

    public enum DescriptionType {
        PROPERTY_OVERVIEW,
        ROOM_DESCRIPTION,
        AMENITIES,
        HOUSE_RULES,
        NEIGHBORHOOD,
        GUEST_ACCESS
    }

    public enum GenerationStatus {
        PENDING,
        GENERATING,
        COMPLETED,
        FAILED,
        APPROVED,
        REJECTED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        approved = false;
        isActive = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Explicit getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public DescriptionType getDescriptionType() { return descriptionType; }
    public void setDescriptionType(DescriptionType descriptionType) { this.descriptionType = descriptionType; }

    public String getOriginalDescription() { return originalDescription; }
    public void setOriginalDescription(String originalDescription) { this.originalDescription = originalDescription; }

    public String getGeneratedDescription() { return generatedDescription; }
    public void setGeneratedDescription(String generatedDescription) { this.generatedDescription = generatedDescription; }

    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }

    public GenerationStatus getStatus() { return status; }
    public void setStatus(GenerationStatus status) { this.status = status; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; }

    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Integer getCharacterCount() { return characterCount; }
    public void setCharacterCount(Integer characterCount) { this.characterCount = characterCount; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
}
