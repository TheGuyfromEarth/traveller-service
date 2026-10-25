package com.travolish.traveller.listing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Step1Request {
    @NotBlank
    private String category;
}
