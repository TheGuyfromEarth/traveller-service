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
public class RefundRequest {
    
    @NotNull(message = "Payment ID is required")
    @Positive(message = "Payment ID must be positive")
    private Long paymentId;
    
    @NotNull(message = "Refund amount is required")
    @Positive(message = "Refund amount must be positive")
    @DecimalMin(value = "0.01", message = "Minimum refund amount is 0.01")
    private BigDecimal refundAmount;
    
    @NotBlank(message = "Refund reason is required")
    private String reason;                          // Booking cancellation, Guest request, Dispute, etc.
    
    private String notes;
    private String metadata;
}
