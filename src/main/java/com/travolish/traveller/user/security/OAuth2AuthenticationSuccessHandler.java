package com.travolish.traveller.user.security;

import java.io.IOException;
import java.util.Map;

import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            Map<String, Object> attributes = oauth2User.getAttributes();
            // For Google, 'email' and 'sub' are present
            String email = (String) attributes.get("email");
            String providerId = attributes.get("sub") != null ? attributes.get("sub").toString() : null;

            User user = null;
            if (email != null) {
                user = userRepository.findByEmail(email).orElse(null);
            }

            if (user == null) {
                user = User.builder()
                        .email(email)
                        .firstName((String) attributes.get("given_name"))
                        .lastName((String) attributes.get("family_name"))
                        .provider("google")
                        .providerId(providerId)
                        .build();
                userRepository.save(user);
            } else {
                // update provider info if missing
                boolean changed = false;
                if (user.getProvider() == null && providerId != null) {
                    user.setProvider("google");
                    user.setProviderId(providerId);
                    changed = true;
                }
                if (changed) userRepository.save(user);
            }

            // Respond with JSON containing basic user info (for single-page apps)
            response.setContentType("application/json;charset=UTF-8");
            String json = String.format("{\"id\":%d,\"email\":\"%s\",\"firstName\":\"%s\",\"lastName\":\"%s\"}",
                    user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
            response.getWriter().write(json);
            response.getWriter().flush();
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}
