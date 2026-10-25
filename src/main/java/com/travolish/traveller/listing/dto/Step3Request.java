package com.travolish.traveller.listing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Step3Request {
    @NotBlank(message = "Property name is required")
    private String name;

    private Integer starRating;

    private Integer numBedrooms;

    private Integer numBathrooms;

    @NotNull(message = "Maximum guests is required")
    @Min(value = 1, message = "Must allow at least 1 guest")
    private Integer maxGuests;

    @NotNull(message = "Number of units/rooms is required")
    @Min(value = 1, message = "Must have at least 1 unit")
    private Integer numUnits;

    private String checkInTime;

    private String checkOutTime;
}
