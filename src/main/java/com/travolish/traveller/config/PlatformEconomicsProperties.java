package com.travolish.traveller.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "travolish.economics")
public class PlatformEconomicsProperties {

    private BigDecimal platformFeePercent = new BigDecimal("2.5");
    private BigDecimal gstPercent = new BigDecimal("18");
    private String currency = "INR";
    private BigDecimal minPayoutAmount = new BigDecimal("100");
    private String payoutMode = "IMPS";
}
