package com.travolish.traveller.payment.entity;

public enum RefundStatus {
    PENDING,        // Refund requested, awaiting processing
    PROCESSING,     // Refund being processed
    COMPLETED,      // Refund completed successfully
    FAILED          // Refund failed
}
