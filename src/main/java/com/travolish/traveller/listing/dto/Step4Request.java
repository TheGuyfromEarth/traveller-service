package com.travolish.traveller.listing.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Step4Request {
    private List<String> amenities;

    private List<String> targetGuests;

    @NotBlank(message = "Stay type is required")
    private String stayType;
}
