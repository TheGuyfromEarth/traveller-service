package com.travolish.traveller.inventory.exception;

public class InvalidPricingRuleException extends RuntimeException {
    
    public InvalidPricingRuleException(String message) {
        super(message);
    }

    public InvalidPricingRuleException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidPricingRuleException invalidDateRange() {
        return new InvalidPricingRuleException(
            "Invalid date range: start date must be before end date"
        );
    }

    public static InvalidPricingRuleException invalidPrice(Double price) {
        return new InvalidPricingRuleException(
            String.format("Invalid price: %s. Price must be greater than 0", price)
        );
    }

    public static InvalidPricingRuleException invalidMultiplier(Double multiplier) {
        return new InvalidPricingRuleException(
            String.format("Invalid multiplier: %s. Multiplier must be positive", multiplier)
        );
    }

    public static InvalidPricingRuleException ruleNotFound(Long ruleId) {
        return new InvalidPricingRuleException(
            String.format("Pricing rule not found: %d", ruleId)
        );
    }
}
