package com.travolish.traveller.user.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.travolish.traveller.notifications.service.EmailService;
import com.travolish.traveller.user.dto.UserDTO;
import com.travolish.traveller.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final EmailService emailService;

    private final UserService userService;

    /**
     * Returns the backend user record for the currently authenticated session.
     * Token subject is the backend user ID set by JwtTokenProvider.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getMe(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        return new ResponseEntity<>(userService.createUser(userDTO), HttpStatus.CREATED);
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserDTO> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "50") int size) {
        if (page != null) {
            PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            return ResponseEntity.ok(userService.getUsersPage(pageable));
        }
        if (role != null || status != null) {
            return ResponseEntity.ok(userService.getUsersByFilter(role, status));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDTO> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.updateUserStatus(id, body.get("status")));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.updateUserRole(id, body.get("role")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Send an admin notice to a user. Accepts a { message } body.
     * Returns 200 with a confirmation payload; wiring to an email or push
     * notification service can be added here when the infra is ready.
     */
    @PostMapping("/{id}/notify")
    public ResponseEntity<Map<String, String>> notifyUser(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        UserDTO user = userService.getUserById(id);
        String message = body.getOrDefault("message", "");
        String subject = body.getOrDefault("subject", "Message from Travolish Admin");
        try {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                emailService.sendSimpleEmail(user.getEmail(), subject, message);
            } else {
                log.warn("User {} has no email address — notice not delivered", id);
            }
        } catch (Exception e) {
            log.error("Failed to send notice to user {}: {}", id, e.getMessage());
        }
        return ResponseEntity.ok(Map.of(
            "status", "sent",
            "userId", String.valueOf(id),
            "message", message
        ));
    }
}
