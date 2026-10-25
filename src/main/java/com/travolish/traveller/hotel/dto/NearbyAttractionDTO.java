package com.travolish.traveller.hotel.dto;

import com.travolish.traveller.hotel.model.NearbyAttraction;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyAttractionDTO {
    private Long id;
    private Long hotelId;
    private String name;
    private String distanceText;
    private NearbyAttraction.AttractionType attractionType;
}
