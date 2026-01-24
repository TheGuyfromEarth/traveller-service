package com.travolish.traveller.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    
    @NotNull(message = "Booking ID is required")
    @Positive(message = "Booking ID must be positive")
    private Long bookingId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMin(value = "1.00", message = "Minimum amount is 1.00")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter code (e.g., USD, INR)")
    private String currency;
    
    @NotNull(message = "Payment method ID is required")
    @Positive(message = "Payment method ID must be positive")
    private Long paymentMethodId;
    
    private String description;
    
    @NotBlank(message = "Save payment method preference is required")
    private String saveMethod;                      // "YES" or "NO"
    
    private String ipAddress;
    private String userAgent;
    
    private String metadata;                        // JSON string for additional data
}
