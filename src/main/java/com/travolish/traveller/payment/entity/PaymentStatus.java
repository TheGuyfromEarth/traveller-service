package com.travolish.traveller.payment.entity;

public enum PaymentStatus {
    PENDING,        // Payment initiated, awaiting confirmation
    PROCESSING,     // Payment being processed
    COMPLETED,      // Payment successful
    FAILED,         // Payment failed
    CANCELLED,      // Payment cancelled by user
    REFUNDED        // Payment refunded
}
