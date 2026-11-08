package com.travolish.traveller.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodDTO {
    
    private Long id;
    private Long userId;
    
    private String methodType;                      // CARD, UPI, NETBANKING, etc.
    private String methodName;
    private Boolean isDefault;
    private Boolean isActive;
    
    // Card Details
    private String cardLast4;
    private String cardNetwork;
    private String cardHolderName;
    private Integer cardExpiryMonth;
    private Integer cardExpiryYear;
    private String cardIssuerBank;
    private String cardType;
    
    // UPI Details
    private String upiVpa;
    
    // Verification
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    
    // Usage
    private Integer usageCount;
    private LocalDateTime lastUsedAt;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
