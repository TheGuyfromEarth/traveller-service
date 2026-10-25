package com.travolish.traveller.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceiptDTO {
    
    private Long id;
    private Long paymentId;
    private Long userId;
    private Long bookingId;
    private Long hostId;
    
    private String receiptNumber;
    private String receiptStatus;
    
    private BigDecimal baseAmount;
    private BigDecimal taxAmount;
    private BigDecimal platformFee;
    private BigDecimal totalAmount;
    
    private String pdfUrl;
    private String htmlUrl;
    
    private String recipientEmail;
    private String recipientName;
    
    private LocalDateTime generatedAt;
    private LocalDateTime sentAt;
    private LocalDateTime downloadedAt;
    
    private String notes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
