package com.travolish.traveller.wishlist.controller;

import com.travolish.traveller.user.security.AuthSupport;
import com.travolish.traveller.wishlist.dto.WishlistDTO;
import com.travolish.traveller.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<WishlistDTO>> getWishlist(@PathVariable Long userId,
            Authentication authentication) {
        AuthSupport.requireSelfOrAdmin(authentication, userId);
        return ResponseEntity.ok(wishlistService.getWishlistForUser(userId));
    }

    @GetMapping("/{userId}/{hotelId}")
    public ResponseEntity<Map<String, Boolean>> checkStatus(
            @PathVariable Long userId, @PathVariable Long hotelId,
            Authentication authentication) {
        AuthSupport.requireSelfOrAdmin(authentication, userId);
        return ResponseEntity.ok(wishlistService.checkStatus(userId, hotelId));
    }

    @PostMapping("/{userId}/{hotelId}")
    public ResponseEntity<WishlistDTO> addToWishlist(
            @PathVariable Long userId, @PathVariable Long hotelId,
            Authentication authentication) {
        AuthSupport.requireSelfOrAdmin(authentication, userId);
        return ResponseEntity.ok(wishlistService.addToWishlist(userId, hotelId));
    }

    @DeleteMapping("/{userId}/{hotelId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long userId, @PathVariable Long hotelId,
            Authentication authentication) {
        AuthSupport.requireSelfOrAdmin(authentication, userId);
        wishlistService.removeFromWishlist(userId, hotelId);
        return ResponseEntity.noContent().build();
    }
}
