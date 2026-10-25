package com.travolish.traveller.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentValidationRequest {
    
    @NotNull(message = "Razorpay order ID is required")
    @NotBlank(message = "Razorpay order ID cannot be blank")
    private String razorpayOrderId;
    
    @NotNull(message = "Razorpay payment ID is required")
    @NotBlank(message = "Razorpay payment ID cannot be blank")
    private String razorpayPaymentId;
    
    @NotNull(message = "Razorpay signature is required")
    @NotBlank(message = "Razorpay signature cannot be blank")
    private String razorpaySignature;
    
    private String metadata;
}
