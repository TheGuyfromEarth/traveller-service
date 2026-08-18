package com.travolish.traveller.wishlist.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistDTO {
    private Long id;
    private Long userId;
    private Long hotelId;
    private OffsetDateTime createdAt;
}
