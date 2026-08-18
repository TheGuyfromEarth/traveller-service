package com.travolish.traveller.wishlist.service;

import com.travolish.traveller.wishlist.dto.WishlistDTO;
import com.travolish.traveller.wishlist.entity.Wishlist;
import com.travolish.traveller.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;

    @Transactional
    public WishlistDTO addToWishlist(Long userId, Long hotelId) {
        if (wishlistRepository.existsByUserIdAndHotelId(userId, hotelId)) {
            return wishlistRepository.findByUserIdAndHotelId(userId, hotelId)
                    .map(this::toDTO).orElseThrow();
        }
        Wishlist saved = wishlistRepository.save(
                Wishlist.builder().userId(userId).hotelId(hotelId).build());
        return toDTO(saved);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long hotelId) {
        wishlistRepository.deleteByUserIdAndHotelId(userId, hotelId);
    }

    public List<WishlistDTO> getWishlistForUser(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Map<String, Boolean> checkStatus(Long userId, Long hotelId) {
        return Map.of("wishlisted", wishlistRepository.existsByUserIdAndHotelId(userId, hotelId));
    }

    private WishlistDTO toDTO(Wishlist w) {
        return WishlistDTO.builder()
                .id(w.getId())
                .userId(w.getUserId())
                .hotelId(w.getHotelId())
                .createdAt(w.getCreatedAt())
                .build();
    }
}
