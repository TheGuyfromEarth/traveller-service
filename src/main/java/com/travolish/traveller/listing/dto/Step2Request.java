package com.travolish.traveller.listing.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Step2Request {
    @NotEmpty(message = "Select at least one property sub-type")
    private List<String> subTypes;
}
