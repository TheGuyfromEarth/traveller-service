package com.travolish.traveller.payout.service;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutReleaseScheduler {

    private final HostLedgerService hostLedgerService;

    @Scheduled(cron = "0 30 3 * * *")
    public void releaseAfterCheckout() {
        try {
            hostLedgerService.releaseEligibleEarnings(LocalDate.now());
        } catch (Exception e) {
            log.error("Payout release scheduler failed: {}", e.getMessage(), e);
        }
    }
}
