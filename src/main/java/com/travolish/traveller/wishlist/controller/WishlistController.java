package com.travolish.traveller.wishlist.controller;

import com.travolish.traveller.wishlist.dto.WishlistDTO;
import com.travolish.traveller.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // GET /api/wishlists/{userId} — all wishlisted hotels for a user
    @GetMapping("/{userId}")
    public ResponseEntity<List<WishlistDTO>> getWishlist(@PathVariable Long userId) {
        return ResponseEntity.ok(wishlistService.getWishlistForUser(userId));
    }

    // GET /api/wishlists/{userId}/{hotelId} — check if a hotel is wishlisted
    @GetMapping("/{userId}/{hotelId}")
    public ResponseEntity<Map<String, Boolean>> checkStatus(
            @PathVariable Long userId, @PathVariable Long hotelId) {
        return ResponseEntity.ok(wishlistService.checkStatus(userId, hotelId));
    }

    // POST /api/wishlists/{userId}/{hotelId} — add to wishlist
    @PostMapping("/{userId}/{hotelId}")
    public ResponseEntity<WishlistDTO> addToWishlist(
            @PathVariable Long userId, @PathVariable Long hotelId) {
        return ResponseEntity.ok(wishlistService.addToWishlist(userId, hotelId));
    }

    // DELETE /api/wishlists/{userId}/{hotelId} — remove from wishlist
    @DeleteMapping("/{userId}/{hotelId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long userId, @PathVariable Long hotelId) {
        wishlistService.removeFromWishlist(userId, hotelId);
        return ResponseEntity.noContent().build();
    }
}
