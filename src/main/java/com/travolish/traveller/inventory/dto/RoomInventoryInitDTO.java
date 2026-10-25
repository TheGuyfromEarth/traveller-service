package com.travolish.traveller.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomInventoryInitDTO {
    private Long roomId;
    private Integer roomCount;
    private Double basePrice;
    private String roomType;
}
