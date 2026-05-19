package com.travolish.traveller.hotel.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSearchResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String country;
    private Double rating;
    private Integer reviewCount;
    private String phone;
    private String email;
    private String description;
    private OffsetDateTime createdAt;

}
