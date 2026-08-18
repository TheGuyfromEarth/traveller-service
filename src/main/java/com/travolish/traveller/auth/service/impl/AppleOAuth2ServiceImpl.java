package com.travolish.traveller.auth.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travolish.traveller.auth.dto.AppleOAuth2Request;
import com.travolish.traveller.auth.dto.AppleTokenResponse;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;
import com.travolish.traveller.auth.service.AppleOAuth2Service;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import com.travolish.traveller.auth.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Date;
import java.util.Scanner;

@Slf4j
@Service
@Transactional
public class AppleOAuth2ServiceImpl implements AppleOAuth2Service {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${apple.client-id:com.travolish.app}")
    private String appleClientId;

    @Value("${apple.team-id:ABCD123456}")
    private String appleTeamId;

    @Value("${apple.key-id:ABCD123456}")
    private String appleKeyId;

    @Value("${apple.key-path:/path/to/apple/key.p8}")
    private String appleKeyPath;

    @Value("${apple.bundle-id:com.travolish}")
    private String appleBundleId;

    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";
    private static final String APPLE_KEY_URL = "https://appleid.apple.com/auth/keys";

    @Override
    public AppleTokenResponse exchangeCodeForToken(String code) {
        log.info("Exchanging Apple authorization code for tokens");
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", appleClientId);
            params.add("client_secret", generateClientSecret());
            params.add("code", code);
            params.add("grant_type", "authorization_code");
            params.add("redirect_uri", "https://api.travolish.com/api/auth/apple/callback");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<AppleTokenResponse> response = restTemplate.postForEntity(
                    APPLE_TOKEN_URL,
                    request,
                    AppleTokenResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully exchanged Apple authorization code for tokens");
                return response.getBody();
            }

            throw new RuntimeException("Failed to exchange Apple authorization code");
        } catch (Exception e) {
            log.error("Error exchanging Apple authorization code", e);
            throw new RuntimeException("Apple token exchange failed: " + e.getMessage(), e);
        }
    }

    @Override
    public AppleOAuth2Request.AppleUserData getUserInfoFromIdToken(String idToken) {
        log.info("Extracting user info from Apple ID token");
        try {
            // Verify the ID token
            if (!verifyIdToken(idToken)) {
                throw new SecurityException("Invalid Apple ID token signature");
            }

            // Decode the ID token to get claims
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getApplePublicKey())
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();

            AppleOAuth2Request.AppleUserData userData = new AppleOAuth2Request.AppleUserData();
            userData.setName(extractName(claims));
            userData.setEmail(claims.get("email", String.class));

            log.debug("Extracted Apple user: {}", userData.getEmail());
            return userData;
        } catch (Exception e) {
            log.error("Error extracting user info from Apple ID token", e);
            throw new RuntimeException("Failed to extract user info from ID token: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public OAuth2AuthResponse handleAppleCallback(AppleOAuth2Request request) {
        log.info("Handling Apple OAuth2 callback");
        try {
            // Exchange code for tokens
            AppleTokenResponse tokenResponse = exchangeCodeForToken(request.getCode());

            // Get user info from ID token
            AppleOAuth2Request.AppleUserData userInfo;
            if (request.getUser() != null) {
                // First-time signup - user data provided by Apple
                userInfo = request.getUser();
            } else {
                // Subsequent logins - extract from ID token
                userInfo = getUserInfoFromIdToken(tokenResponse.getIdToken());
            }

            // Find or create user
            String appleUserId = extractSubjectFromIdToken(tokenResponse.getIdToken());
            User user = userRepository.findByProviderAndProviderId("apple", appleUserId)
                    .orElseGet(() -> createAppleUser(userInfo, appleUserId));

            // Update user with latest information
            // Note: Token expiry is handled by the token response
            userRepository.save(user);

            // Generate JWT tokens
            String jwtToken = jwtTokenProvider.generateToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            log.info("Successfully handled Apple OAuth2 callback for user: {}", user.getId());
            return OAuth2AuthResponse.builder()
                    .userId(user.getId())
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .isNewUser(false) // Assuming update, not new user
                    .build();
        } catch (Exception e) {
            log.error("Error handling Apple OAuth2 callback", e);
            throw new RuntimeException("Apple OAuth2 callback failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyIdToken(String idToken) {
        log.debug("Verifying Apple ID token signature");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getApplePublicKey())
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();

            // Verify essential claims
            if (!claims.getAudience().contains(appleClientId)) {
                log.warn("ID token audience mismatch");
                return false;
            }

            if (claims.getExpiration().before(new Date())) {
                log.warn("ID token expired");
                return false;
            }

            log.debug("Apple ID token signature verified successfully");
            return true;
        } catch (SignatureException e) {
            log.error("Apple ID token signature verification failed", e);
            return false;
        } catch (Exception e) {
            log.error("Error verifying Apple ID token", e);
            return false;
        }
    }

    @Override
    public boolean revokeRefreshToken(String refreshToken) {
        log.info("Revoking Apple refresh token");
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", appleClientId);
            params.add("client_secret", generateClientSecret());
            params.add("token", refreshToken);
            params.add("token_type_hint", "refresh_token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    APPLE_REVOKE_URL,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Apple refresh token revoked successfully");
                return true;
            }

            log.warn("Failed to revoke Apple refresh token");
            return false;
        } catch (Exception e) {
            log.error("Error revoking Apple refresh token", e);
            return false;
        }
    }

    // Helper methods

    private User createAppleUser(AppleOAuth2Request.AppleUserData userInfo, String appleUserId) {
        User user = new User();
        user.setProvider("apple");
        user.setProviderId(appleUserId);
        user.setEmail(userInfo.getEmail());
        
        if (userInfo.getName() != null) {
            String[] names = userInfo.getName().split(" ", 2);
            user.setFirstName(names[0]);
            user.setLastName(names.length > 1 ? names[1] : "");
        }

        log.info("Creating new Apple user: {}", userInfo.getEmail());
        return userRepository.save(user);
    }

    private String extractSubjectFromIdToken(String idToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getApplePublicKey())
                    .build()
                    .parseClaimsJws(idToken)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting subject from ID token", e);
            throw new RuntimeException("Failed to extract subject from ID token");
        }
    }

    private String extractName(Claims claims) {
        Object nameObj = claims.get("name");
        if (nameObj != null) {
            return nameObj.toString();
        }
        // Try to construct from given_name and family_name
        String givenName = claims.get("given_name", String.class);
        String familyName = claims.get("family_name", String.class);
        if (givenName != null) {
            return familyName != null ? givenName + " " + familyName : givenName;
        }
        return null;
    }

    private String generateClientSecret() {
        // This should be generated using Apple's private key
        // Implementation depends on your key storage strategy
        // For now, returning a placeholder
        try {
            // Read private key from file path
            String privateKeyContent = readPrivateKey(appleKeyPath);
            // Generate JWT client secret using the private key
            return Jwts.builder()
                    .setIssuer(appleTeamId)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 600000)) // 10 minutes
                    .setAudience(APPLE_TOKEN_URL)
                    .setSubject(appleClientId)
                    .claim("kid", appleKeyId)
                    .signWith(getApplePrivateKey(privateKeyContent))
                    .compact();
        } catch (Exception e) {
            log.error("Error generating Apple client secret", e);
            throw new RuntimeException("Failed to generate Apple client secret: " + e.getMessage(), e);
        }
    }

    private String readPrivateKey(String keyPath) throws Exception {
        StringBuilder content = new StringBuilder();
        try (Scanner scanner = new Scanner(new java.io.File(keyPath))) {
            while (scanner.hasNextLine()) {
                content.append(scanner.nextLine()).append("\n");
            }
        }
        return content.toString();
    }

    private PublicKey getApplePublicKey() throws Exception {
        // Fetch Apple's public keys from their endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(APPLE_KEY_URL, String.class);
        JsonNode keysNode = objectMapper.readTree(response.getBody());

        // Find the appropriate key and construct PublicKey object
        // This is a simplified implementation
        JsonNode keys = keysNode.get("keys");
        if (keys != null && keys.isArray() && keys.size() > 0) {
            // Use the first key (production should match kid)
            JsonNode key = keys.get(0);
            return constructPublicKey(key);
        }

        throw new RuntimeException("No valid Apple public keys found");
    }

    private PublicKey constructPublicKey(JsonNode keyNode) throws Exception {
        // Construct PublicKey from JWK format
        // This is a simplified implementation - production code should use proper JWK parsing library
        String modulus = keyNode.get("n").asText();
        String exponent = keyNode.get("e").asText();

        byte[] decodedModulus = Base64.getUrlDecoder().decode(modulus);
        byte[] decodedExponent = Base64.getUrlDecoder().decode(exponent);

        java.math.BigInteger mod = new java.math.BigInteger(1, decodedModulus);
        java.math.BigInteger exp = new java.math.BigInteger(1, decodedExponent);

        java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(mod, exp);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    private PrivateKey getApplePrivateKey(String keyContent) throws Exception {
        // Parse PEM private key content
        // Remove PEM headers and whitespace
        String privateKeyPEM = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] decodedKey = Base64.getDecoder().decode(privateKeyPEM);
        java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(decodedKey);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }
}
