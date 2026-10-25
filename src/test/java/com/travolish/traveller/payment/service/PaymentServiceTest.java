package com.travolish.traveller.payment.service;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.model.BookingPaymentConfig;
import com.travolish.traveller.booking.repository.BookingPaymentConfigRepository;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.booking.service.BookingAccessService;
import com.travolish.traveller.config.PlatformEconomicsProperties;
import com.travolish.traveller.payout.service.HostLedgerService;
import com.travolish.traveller.payment.entity.Payment;
import com.travolish.traveller.payment.entity.PaymentStatus;
import com.travolish.traveller.payment.entity.Receipt;
import com.travolish.traveller.payment.repository.PaymentRepository;
import com.travolish.traveller.payment.repository.PaymentMethodRepository;
import com.travolish.traveller.payment.repository.ReceiptRepository;
import com.travolish.traveller.payment.repository.RefundRepository;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMethodRepository paymentMethodRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private ReceiptRepository receiptRepository;
    @Mock private RazorpayIntegrationService razorpayService;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingPaymentConfigRepository bookingPaymentConfigRepository;
    @Mock private BookingAccessService bookingAccessService;
    @Mock private HostLedgerService hostLedgerService;
    @Mock private ModelMapper modelMapper;
    @Spy private PlatformEconomicsProperties economicsProperties = new PlatformEconomicsProperties();

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setKey() {
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "rzp_test");
    }

    @Test
    void computeCheckoutAmounts_fullBooking_defaultConfig() {
        Booking booking = new Booking();
        booking.setHotelId(10L);
        booking.setTotalPrice(1000.0);

        when(bookingPaymentConfigRepository.findByHotelId(10L)).thenReturn(Optional.empty());

        PaymentCheckoutAmounts amounts = paymentService.computeCheckoutAmountsForBooking(booking);

        assertEquals(0, amounts.getBaseAmount().compareTo(new BigDecimal("1000")));
        assertEquals(0, amounts.getPlatformFee().compareTo(new BigDecimal("25")));
        assertEquals(0, amounts.getTaxAmount().compareTo(new BigDecimal("184.5")));
        assertEquals(0, amounts.getTotalAmount().compareTo(new BigDecimal("1209.5")));
    }

    @Test
    void computeCheckoutAmounts_advancePercent() {
        Booking booking = new Booking();
        booking.setHotelId(11L);
        booking.setTotalPrice(2000.0);

        BookingPaymentConfig config = BookingPaymentConfig.builder()
            .hotelId(11L)
            .secureWithPartialPayment(true)
            .advancePaymentPercent(25)
            .payFullAtBooking(false)
            .build();
        when(bookingPaymentConfigRepository.findByHotelId(11L)).thenReturn(Optional.of(config));

        PaymentCheckoutAmounts amounts = paymentService.computeCheckoutAmountsForBooking(booking);

        assertEquals(0, amounts.getBaseAmount().compareTo(new BigDecimal("500.00")));
    }

    @Test
    void computeCheckoutAmounts_fractionalAdvance_matchesPaiseRounding() {
        Booking booking = new Booking();
        booking.setHotelId(12L);
        booking.setTotalPrice(1001.0);

        BookingPaymentConfig config = BookingPaymentConfig.builder()
            .hotelId(12L)
            .secureWithPartialPayment(true)
            .advancePaymentPercent(25)
            .payFullAtBooking(false)
            .build();
        when(bookingPaymentConfigRepository.findByHotelId(12L)).thenReturn(Optional.of(config));

        PaymentCheckoutAmounts amounts = paymentService.computeCheckoutAmountsForBooking(booking);

        assertEquals(0, amounts.getBaseAmount().compareTo(new BigDecimal("250.25")));
        assertEquals(0, amounts.getPlatformFee().compareTo(new BigDecimal("6.26")));
        assertEquals(0, amounts.getTaxAmount().compareTo(new BigDecimal("46.17")));
        assertEquals(0, amounts.getTotalAmount().compareTo(new BigDecimal("302.68")));
        assertEquals(30268L, PaymentService.toPaise(amounts.getTotalAmount()));
    }

    @Test
    void computeCheckoutAmounts_payAtPropertyOnly_throws() {
        Booking booking = new Booking();
        booking.setHotelId(12L);
        booking.setTotalPrice(500.0);

        BookingPaymentConfig config = BookingPaymentConfig.builder()
            .hotelId(12L)
            .payAtProperty(true)
            .payFullAtBooking(false)
            .secureWithPartialPayment(false)
            .build();
        when(bookingPaymentConfigRepository.findByHotelId(12L)).thenReturn(Optional.of(config));

        assertThrows(IllegalStateException.class,
            () -> paymentService.computeCheckoutAmountsForBooking(booking));
    }

    @Test
    void createReceiptIfAbsent_skipsWhenReceiptExists() {
        Payment payment = Payment.builder().id(99L).userId(1L).bookingId(2L).build();
        when(receiptRepository.findByPaymentId(99L)).thenReturn(Optional.of(new Receipt()));

        ReflectionTestUtils.invokeMethod(paymentService, "createReceiptIfAbsent", payment);

        verify(receiptRepository, never()).save(any());
    }

    @Test
    void validatePayment_idempotentWhenAlreadyCompleted() {
        Payment payment = Payment.builder()
            .id(1L)
            .userId(5L)
            .paymentStatus(PaymentStatus.COMPLETED)
            .razorpayOrderId("order_1")
            .build();
        when(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(payment));
        when(modelMapper.map(payment, com.travolish.traveller.payment.dto.PaymentDTO.class))
            .thenReturn(new com.travolish.traveller.payment.dto.PaymentDTO());

        paymentService.validatePayment(5L, false,
            com.travolish.traveller.payment.dto.PaymentValidationRequest.builder()
                .razorpayOrderId("order_1")
                .razorpayPaymentId("pay_1")
                .razorpaySignature("sig")
                .build());

        verify(razorpayService, never()).getPaymentDetails(any());
    }

    @Test
    void applyCapturedPayment_verifiesAmount() {
        Payment payment = Payment.builder()
            .id(3L)
            .totalAmount(new BigDecimal("100.00"))
            .paymentStatus(PaymentStatus.PROCESSING)
            .build();
        JSONObject details = new JSONObject();
        details.put("amount", 9999L);
        details.put("id", "pay_x");

        assertThrows(IllegalStateException.class, () ->
            ReflectionTestUtils.invokeMethod(paymentService, "applyCapturedPayment",
                payment, "pay_x", details));
    }
}
