package com.travolish.traveller.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelSearchRequest {

    private String country;
    private String city;
    private String name;
    private Double minRating;
    private Double maxRating;
    @Default
    private Integer pageNumber = 0;
    @Default
    private Integer pageSize = 10;

}
