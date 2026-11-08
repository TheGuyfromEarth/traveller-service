package com.travolish.traveller.user.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/**
 * JWT subject and role helpers shared by controllers.
 */
public final class AuthSupport {

    private AuthSupport() {}

    public static Long userId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        try {
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = (Jwt) jwtAuth.getPrincipal();
                String subject = jwt.getSubject();
                if (subject != null && !subject.isBlank()) {
                    return Long.parseLong(subject);
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    public static Long requireUserId(Authentication authentication) {
        Long id = userId(authentication);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return id;
    }

    public static void requireSelfOrAdmin(Authentication authentication, Long targetUserId) {
        if (isAdmin(authentication)) {
            return;
        }
        Long authUserId = requireUserId(authentication);
        if (targetUserId == null || !authUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this resource");
        }
    }

    public static String role(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = (Jwt) jwtAuth.getPrincipal();
            return jwt.getClaimAsString("role");
        }
        return null;
    }

    public static boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || roleName == null) {
            return false;
        }
        String authority = "ROLE_" + roleName.toUpperCase();
        for (GrantedAuthority ga : authentication.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ADMIN");
    }
}
