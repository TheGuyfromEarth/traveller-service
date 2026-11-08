package com.travolish.traveller.payment.service;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentCheckoutAmounts {
    BigDecimal baseAmount;
    BigDecimal platformFee;
    BigDecimal taxAmount;
    BigDecimal totalAmount;
    BigDecimal netAmount;
}
