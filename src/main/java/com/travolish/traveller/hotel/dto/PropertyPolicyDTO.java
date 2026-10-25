package com.travolish.traveller.hotel.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyPolicyDTO {
    private Long id;
    private Long hotelId;
    private String cancellationPolicy;
    private String refundPolicy;
    private String childPolicy;
    private String petPolicy;
    private String smokingPolicy;
    private String visitorPolicy;
    private String damagePolicy;
    private String quietHours;
}
