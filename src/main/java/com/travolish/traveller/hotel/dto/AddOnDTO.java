package com.travolish.traveller.hotel.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddOnDTO {
    private Long id;
    private String title;
    private String description;
    private Double price;
    private Long hotelId;
}
