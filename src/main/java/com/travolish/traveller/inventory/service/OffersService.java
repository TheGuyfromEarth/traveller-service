package com.travolish.traveller.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.inventory.dto.OfferDTO;
import com.travolish.traveller.inventory.dto.TravelCreditsDTO;
import com.travolish.traveller.inventory.model.PricingRule;
import com.travolish.traveller.inventory.repository.PricingRuleRepository;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OffersService {

    private static final List<String> OFFER_RULE_TYPES = Arrays.asList(
        "PROMOTIONAL", "EARLY_BIRD", "LAST_MINUTE", "LOYALTY"
    );
    private static final double CREDIT_RATE = 0.02; // 2% cashback on completed bookings
    private static final int EXPIRY_WARNING_DAYS = 30;

    private final PricingRuleRepository pricingRuleRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /** Return all active deal-type pricing rules as traveller-facing offers. */
    public List<OfferDTO> getActiveOffers() {
        LocalDate today = LocalDate.now();
        List<PricingRule> rules = pricingRuleRepository.findAll().stream()
            .filter(r -> r.getIsActive()
                      && OFFER_RULE_TYPES.contains(r.getRuleType().name())
                      && !today.isAfter(r.getEndDate()))
            .collect(Collectors.toList());

        Map<Long, String> hotelNameMap = batchFetchHotelNames(rules);
        return rules.stream()
            .map(r -> mapToOffer(r, today, hotelNameMap))
            .collect(Collectors.toList());
    }

    /** Find an offer by promo code (case-insensitive). */
    public OfferDTO validatePromoCode(String code) {
        if (code == null || code.isBlank()) return null;
        LocalDate today = LocalDate.now();

        List<PricingRule> allRules = pricingRuleRepository.findAll();
        Map<Long, String> hotelNameMap = batchFetchHotelNames(allRules);

        return allRules.stream()
            .filter(r -> r.getIsActive() && !today.isAfter(r.getEndDate()))
            .filter(r -> code.trim().equalsIgnoreCase(effectiveCode(r)))
            .findFirst()
            .map(r -> mapToOffer(r, today, hotelNameMap))
            .orElse(null);
    }

    /**
     * Batch-fetch hotel names for all rules in a single IN-query, returning a
     * map of hotelId → name. Replaces per-rule hotelRepository.findById() calls.
     */
    private Map<Long, String> batchFetchHotelNames(List<PricingRule> rules) {
        Set<Long> hotelIds = rules.stream()
            .map(PricingRule::getHotelId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (hotelIds.isEmpty()) return Collections.emptyMap();
        return hotelRepository.findIdAndNameByIdIn(hotelIds).stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (String) row[1]
            ));
    }

    /** Compute travel credits for a user: 2% of all confirmed booking revenue. */
    public TravelCreditsDTO getUserCredits(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return TravelCreditsDTO.builder()
                .userId(userId)
                .availableCredits(BigDecimal.ZERO)
                .currency("INR")
                .bookingsEarned(0)
                .build();
        }

        List<Booking> confirmed = bookingRepository.findByGuestEmailIgnoreCase(user.getEmail())
            .stream()
            .filter(b -> Booking.BookingStatus.CONFIRMED == b.getStatus() ||
                         Booking.BookingStatus.COMPLETED == b.getStatus())
            .collect(Collectors.toList());

        BigDecimal totalRevenue = confirmed.stream()
            .map(b -> BigDecimal.valueOf(b.getTotalPrice() != null ? b.getTotalPrice() : 0.0))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal credits = totalRevenue
            .multiply(BigDecimal.valueOf(CREDIT_RATE))
            .setScale(0, RoundingMode.HALF_UP);

        return TravelCreditsDTO.builder()
            .userId(userId)
            .availableCredits(credits)
            .currency("INR")
            .bookingsEarned(confirmed.size())
            .build();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private OfferDTO mapToOffer(PricingRule rule, LocalDate today, Map<Long, String> hotelNameMap) {
        String code = effectiveCode(rule);
        String hotelName = rule.getHotelId() != null ? hotelNameMap.get(rule.getHotelId()) : null;

        String discountSummary = buildDiscountSummary(rule);
        Double discountPercent = null;
        Double fixedDiscount = rule.getFixedDiscount();

        if (rule.getMultiplier() != null && rule.getMultiplier() < 1.0) {
            discountPercent = Math.round((1.0 - rule.getMultiplier()) * 1000.0) / 10.0;
        } else if (rule.getMultiplier() != null && rule.getMultiplier() > 1.0) {
            discountPercent = -Math.round((rule.getMultiplier() - 1.0) * 1000.0) / 10.0;
        }

        boolean expiringSoon = rule.getEndDate() != null &&
            !rule.getEndDate().isAfter(today.plusDays(EXPIRY_WARNING_DAYS));

        return OfferDTO.builder()
            .id(rule.getId())
            .title(rule.getDescription())
            .ruleType(rule.getRuleType().name())
            .promoCode(code)
            .discountSummary(discountSummary)
            .discountPercent(discountPercent)
            .fixedDiscount(fixedDiscount)
            .validFrom(rule.getStartDate())
            .validUntil(rule.getEndDate())
            .hotelId(rule.getHotelId())
            .hotelName(hotelName)
            .expiringSoon(expiringSoon)
            .active(rule.getIsActive())
            .build();
    }

    private String effectiveCode(PricingRule rule) {
        if (rule.getPromoCode() != null && !rule.getPromoCode().isBlank()) {
            return rule.getPromoCode().toUpperCase();
        }
        // Auto-generate from description: first letter of each word, max 6 chars + id
        String[] words = rule.getDescription().toUpperCase()
            .replaceAll("[^A-Z0-9\\s]", "").split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty() && sb.length() < 5) sb.append(w.charAt(0));
        }
        return sb + String.valueOf(rule.getId());
    }

    private String buildDiscountSummary(PricingRule rule) {
        if (rule.getFixedDiscount() != null && rule.getFixedDiscount() > 0) {
            return "₹" + rule.getFixedDiscount().longValue() + " off";
        }
        if (rule.getMultiplier() != null) {
            if (rule.getMultiplier() < 1.0) {
                double pct = Math.round((1.0 - rule.getMultiplier()) * 1000.0) / 10.0;
                return pct + "% off";
            } else if (rule.getMultiplier() > 1.0) {
                double pct = Math.round((rule.getMultiplier() - 1.0) * 1000.0) / 10.0;
                return pct + "% premium";
            }
        }
        return "Special offer";
    }
}
