package com.travolish.traveller.booking.dto;

import java.util.List;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingPaymentConfigDTO {
    private Long hotelId;
    private Boolean payFullAtBooking;
    private Boolean payAtProperty;
    private Boolean secureWithPartialPayment;
    private Integer advancePaymentPercent;
    private List<String> acceptedPaymentMethods;
}
