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

    private String query;   // OR-matches hotel name and city; overrides name/city when set
    private String country;
    private String city;
    private String name;
    private Double minRating;
    private Double maxRating;
    private Double latMin;
    private Double latMax;
    private Double lngMin;
    private Double lngMax;
    @Default
    private Integer pageNumber = 0;
    @Default
    private Integer pageSize = 10;

}
