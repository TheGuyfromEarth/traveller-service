package com.travolish.traveller.hotel.dto;

import java.time.LocalDate;

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

    // Availability filter — all three are optional:
    //   checkIn + checkOut  → hotel must have at least one room free for the entire range
    //   guests              → that room must have capacity >= guests
    // Both conditions are evaluated in a single correlated EXISTS subquery so they
    // combine efficiently rather than running two separate passes.
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;

    @Default
    private Integer pageNumber = 0;
    @Default
    private Integer pageSize = 10;

}
