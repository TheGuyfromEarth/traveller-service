package com.travolish.traveller.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class I18nConfig {

    /**
     * Loads messages from src/main/resources/i18n/messages*.properties.
     * Hot-reloads in dev without restart (cache TTL = 60 s).
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
        ms.setBasename("classpath:i18n/messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setCacheSeconds(60);
        ms.setFallbackToSystemLocale(false); // fall back to messages.properties, not OS locale
        return ms;
    }

    /**
     * Resolves locale from the request's Accept-Language header.
     * Correct for stateless JWT APIs — no session or cookie required.
     * Supported: en, ar, fr, hi.  Anything else defaults to en.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(
                Locale.ENGLISH,
                Locale.forLanguageTag("ar"),
                Locale.FRENCH,
                Locale.forLanguageTag("hi")
        ));
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
}
