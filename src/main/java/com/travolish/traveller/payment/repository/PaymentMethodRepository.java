package com.travolish.traveller.payment.repository;

import com.travolish.traveller.payment.entity.PaymentMethod;
import com.travolish.traveller.payment.entity.PaymentMethodType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    
    // Find by user
    List<PaymentMethod> findByUserId(Long userId);
    Page<PaymentMethod> findByUserId(Long userId, Pageable pageable);
    
    // Find by user and type
    List<PaymentMethod> findByUserIdAndMethodType(Long userId, PaymentMethodType methodType);
    
    // Find by user and active status
    List<PaymentMethod> findByUserIdAndIsActive(Long userId, Boolean isActive);
    
    // Find default method for user
    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(Long userId);
    
    // Find by razorpay token
    Optional<PaymentMethod> findByRazorpayTokenId(String razorpayTokenId);
    
    // Find by UPI VPA
    Optional<PaymentMethod> findByUpiVpa(String upiVpa);
    
    // Find by card (last 4 digits and expiry)
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.userId = :userId AND pm.cardLast4 = :last4 AND pm.cardExpiryMonth = :month AND pm.cardExpiryYear = :year")
    Optional<PaymentMethod> findByUserIdAndCard(
        @Param("userId") Long userId,
        @Param("last4") String last4,
        @Param("month") Integer month,
        @Param("year") Integer year
    );
    
    // Count active methods for user
    Long countByUserIdAndIsActive(Long userId, Boolean isActive);
    
    // Find verified methods for user
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.userId = :userId AND pm.isVerified = true AND pm.isActive = true")
    List<PaymentMethod> findVerifiedMethodsByUserId(@Param("userId") Long userId);
    
    // Check if card exists
    boolean existsByUserIdAndCardLast4AndCardExpiryMonthAndCardExpiryYear(
        Long userId, String cardLast4, Integer month, Integer year
    );
}
