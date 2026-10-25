package com.travolish.traveller.payment.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentMaintenanceScheduler {

    private final PaymentService paymentService;

    @Scheduled(fixedRate = 300_000)
    public void cancelExpiredPendingPayments() {
        try {
            paymentService.cancelExpiredPayments();
        } catch (Exception e) {
            log.error("cancelExpiredPendingPayments failed: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 600_000)
    public void retryFailedPayments() {
        try {
            paymentService.retryFailedPayments();
        } catch (Exception e) {
            log.error("retryFailedPayments failed: {}", e.getMessage(), e);
        }
    }
}
