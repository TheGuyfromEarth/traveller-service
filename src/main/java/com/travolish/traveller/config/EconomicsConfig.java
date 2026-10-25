package com.travolish.traveller.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PlatformEconomicsProperties.class)
public class EconomicsConfig {
}
