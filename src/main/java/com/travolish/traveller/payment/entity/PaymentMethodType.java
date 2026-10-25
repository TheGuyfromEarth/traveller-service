package com.travolish.traveller.payment.entity;

public enum PaymentMethodType {
    CARD,               // Credit/Debit Card
    UPI,                // Unified Payments Interface (India)
    NETBANKING,         // Net Banking
    WALLET,             // Digital Wallet (Razorpay Wallet, etc.)
    PAYPAL,             // PayPal
    APPLE_PAY,          // Apple Pay
    GOOGLE_PAY          // Google Pay
}
