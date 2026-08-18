package com.travolish.traveller.hotel.dto;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertySeoMetaDTO {
    private Long hotelId;
    private String pageTitle;
    private String metaDescription;
    private String urlSlug;
    private String schemaJson;
    private String coverImageTitle;
    private List<String> guestMatchTags;
    private Double valueScore;
    private Double locationScore;
    private OffsetDateTime lastGeneratedAt;
}
