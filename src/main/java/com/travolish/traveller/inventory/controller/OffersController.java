package com.travolish.traveller.inventory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.inventory.dto.OfferDTO;
import com.travolish.traveller.inventory.dto.TravelCreditsDTO;
import com.travolish.traveller.inventory.service.OffersService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class OffersController {

    private final OffersService offersService;

    /**
     * List all active traveller-facing offers (PROMOTIONAL, EARLY_BIRD, LAST_MINUTE, LOYALTY).
     * GET /api/offers
     */
    @GetMapping
    public ResponseEntity<List<OfferDTO>> getActiveOffers() {
        return ResponseEntity.ok(offersService.getActiveOffers());
    }

    /**
     * Validate a promo code and return the matching offer details.
     * GET /api/offers/validate?code=WEEKEND18
     */
    @GetMapping("/validate")
    public ResponseEntity<OfferDTO> validatePromoCode(@RequestParam String code) {
        OfferDTO offer = offersService.validatePromoCode(code);
        return offer != null
            ? ResponseEntity.ok(offer)
            : ResponseEntity.notFound().build();
    }

    /**
     * Return travel credits for an authenticated user.
     * GET /api/offers/credits?userId=17
     */
    @GetMapping("/credits")
    public ResponseEntity<TravelCreditsDTO> getUserCredits(@RequestParam Long userId) {
        return ResponseEntity.ok(offersService.getUserCredits(userId));
    }
}
