package com.travolish.traveller.admin.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(String entityType, Long entityId, String action, String details) {
        Long actorId = null;
        String actorName = "system";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = (Jwt) jwtAuth.getPrincipal();
            Object idClaim = jwt.getClaims().get("id");
            if (idClaim instanceof Number n) actorId = n.longValue();
            String first = jwt.getClaimAsString("firstName");
            String last  = jwt.getClaimAsString("lastName");
            String email = jwt.getClaimAsString("email");
            if (first != null || last != null) {
                actorName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
            } else if (email != null) {
                actorName = email;
            }
        }

        auditLogRepository.save(AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actorId(actorId)
                .actorName(actorName)
                .details(details)
                .build());
    }
}
