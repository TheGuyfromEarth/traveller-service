package com.travolish.traveller.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GenerateDescriptionRequest {
    @NotNull(message = "Hotel ID required")
    private Long hotelId;
    
    @NotNull(message = "Room ID required")
    private Long roomId;
    
    @NotBlank(message = "Original description required")
    private String originalDescription;
    
    @NotNull(message = "Description type required")
    private String descriptionType;
    
    @NotNull(message = "Source language required")
    private String sourceLanguage;
    
    @NotNull(message = "Target language required")
    private String targetLanguage;

    public GenerateDescriptionRequest() {}

    public GenerateDescriptionRequest(Long hotelId, Long roomId, String originalDescription,
                                     String descriptionType, String sourceLanguage, String targetLanguage) {
        this.hotelId = hotelId;
        this.roomId = roomId;
        this.originalDescription = originalDescription;
        this.descriptionType = descriptionType;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    public Long getHotelId() { return hotelId; }
    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getOriginalDescription() { return originalDescription; }
    public void setOriginalDescription(String originalDescription) { this.originalDescription = originalDescription; }

    public String getDescriptionType() { return descriptionType; }
    public void setDescriptionType(String descriptionType) { this.descriptionType = descriptionType; }

    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public static GenerateDescriptionRequestBuilder builder() {
        return new GenerateDescriptionRequestBuilder();
    }

    public static class GenerateDescriptionRequestBuilder {
        private Long hotelId;
        private Long roomId;
        private String originalDescription;
        private String descriptionType;
        private String sourceLanguage;
        private String targetLanguage;

        public GenerateDescriptionRequestBuilder hotelId(Long hotelId) {
            this.hotelId = hotelId;
            return this;
        }

        public GenerateDescriptionRequestBuilder roomId(Long roomId) {
            this.roomId = roomId;
            return this;
        }

        public GenerateDescriptionRequestBuilder originalDescription(String originalDescription) {
            this.originalDescription = originalDescription;
            return this;
        }

        public GenerateDescriptionRequestBuilder descriptionType(String descriptionType) {
            this.descriptionType = descriptionType;
            return this;
        }

        public GenerateDescriptionRequestBuilder sourceLanguage(String sourceLanguage) {
            this.sourceLanguage = sourceLanguage;
            return this;
        }

        public GenerateDescriptionRequestBuilder targetLanguage(String targetLanguage) {
            this.targetLanguage = targetLanguage;
            return this;
        }

        public GenerateDescriptionRequest build() {
            return new GenerateDescriptionRequest(hotelId, roomId, originalDescription, descriptionType, sourceLanguage, targetLanguage);
        }
    }
}
