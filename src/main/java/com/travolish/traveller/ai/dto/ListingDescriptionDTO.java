package com.travolish.traveller.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingDescriptionDTO {
    private Long id;
    private Long hotelId;
    private Long roomId;
    private String sourceLanguage;
    private String targetLanguage;
    private String descriptionType;
    private String originalDescription;
    private String generatedDescription;
    private Double qualityScore;
    private Double confidenceScore;
    private String status;
    private Boolean approved;
    private Boolean isActive;
    private Integer characterCount;
    private Integer wordCount;
    private LocalDateTime createdAt;
}
