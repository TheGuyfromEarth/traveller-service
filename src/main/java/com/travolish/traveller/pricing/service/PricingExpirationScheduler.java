package com.travolish.traveller.pricing.service;

import com.travolish.traveller.pricing.repository.PricingSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Expires PENDING pricing suggestions whose suggestedToDate has passed.
 * Runs at 02:15 AM daily (15 min after BookingStatusScheduler to avoid lock contention).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PricingExpirationScheduler {

    private final PricingSuggestionRepository pricingSuggestionRepository;

    @Scheduled(cron = "0 15 2 * * *")
    @Transactional
    public void expireStalesuggestions() {
        int count = pricingSuggestionRepository.expirePendingSuggestions(LocalDate.now());
        if (count > 0) {
            log.info("Expired {} stale PENDING pricing suggestions", count);
        }
    }
}
