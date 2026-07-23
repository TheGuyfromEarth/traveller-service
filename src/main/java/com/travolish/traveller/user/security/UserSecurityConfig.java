package com.travolish.traveller.user.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
public class UserSecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}") String allowedOriginsRaw) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOriginsRaw.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${jwt.secret}") String jwtSecret) {
        SecretKeySpec key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtDecoder jwtDecoder) {
        return new JwtAuthFilter(jwtDecoder);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**").disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Pre-flight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // H2 console (local dev only)
                        .requestMatchers("/h2-console/**").permitAll()
                        // Auth + public read endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/hotels/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rooms/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/addons/**").permitAll()
                        // Admin-only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Any authenticated user can create their first listing (POST /api/hotels exact path).
                        // HotelServiceImpl.create() promotes the user to HOST role in the DB.
                        // Sub-resource operations on existing hotels still require HOST or ADMIN.
                        .requestMatchers(HttpMethod.POST, "/api/hotels").authenticated()
                        // Admin-only hotel operations (must come before the HOST+ADMIN catch-all below)
                        .requestMatchers(HttpMethod.PATCH, "/api/hotels/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/hotels/*/request-documents").hasRole("ADMIN")
                        // Host + admin
                        .requestMatchers(HttpMethod.POST, "/api/hotels/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/hotels/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/hotels/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/hotels/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/rooms/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/rooms/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/rooms/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/rooms/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers("/api/host/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers("/api/kyc/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers("/api/payouts/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers("/api/promotions/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers("/api/pricing/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers("/api/auto-replies/**").hasAnyRole("HOST", "ADMIN")
                        // Analytics: host/admin only; inventory: any authenticated user
                        .requestMatchers("/api/analytics/**").hasAnyRole("HOST", "ADMIN")
                        .requestMatchers("/api/inventory/**").authenticated()
                        // User management mutations — admin only
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/*/notify").hasRole("ADMIN")
                        // Authenticated users (any role)
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/bookings/**").authenticated()
                        .requestMatchers("/api/messages/**").authenticated()
                        .requestMatchers("/api/wishlists/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()
                        // Review moderation actions — admin only
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*/redact").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*/escalate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*/dismiss").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/reviews/*/assign").hasRole("ADMIN")
                        .requestMatchers("/api/reviews/**").authenticated()
                        // Everything else public (search, property detail, etc.)
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
