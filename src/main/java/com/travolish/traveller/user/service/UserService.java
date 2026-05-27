package com.travolish.traveller.user.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import com.travolish.traveller.notifications.service.NotificationService;
import com.travolish.traveller.user.dto.UserDTO;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.exception.UserAlreadyExistsException;
import com.travolish.traveller.user.exception.UserNotFoundException;
import com.travolish.traveller.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public UserDTO createUser(UserDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + userDTO.getEmail() + " already exists");
        }

        User user = User.builder()
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .email(userDTO.getEmail())
                .password(userDTO.getPassword())
                .phone(userDTO.getPhone())
                .build();

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return convertToDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getUsersByFilter(String role, String status) {
        List<User> users;
        if (role != null && status != null) {
            users = userRepository.findByRoleAndStatusWithDefault(role, status);
        } else if (role != null) {
            users = userRepository.findByRoleWithDefault(role);
        } else {
            users = userRepository.findByStatusWithDefault(status);
        }
        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        existingUser.setFirstName(userDTO.getFirstName());
        existingUser.setLastName(userDTO.getLastName());
        existingUser.setPreferredName(userDTO.getPreferredName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhone(userDTO.getPhone());
        existingUser.setCity(userDTO.getCity());
        existingUser.setTimeZone(userDTO.getTimeZone());
        existingUser.setTravelStyle(userDTO.getTravelStyle());
        existingUser.setBio(userDTO.getBio());
        if (userDTO.getAvatarUrl() != null) {
            existingUser.setImageKey(userDTO.getAvatarUrl());
        }
        // Don't update password here, create a separate endpoint for password update

        User updatedUser = userRepository.save(existingUser);
        return convertToDTO(updatedUser);
    }

    public UserDTO updateUserStatus(Long id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        user.setStatus(status);
        return convertToDTO(userRepository.save(user));
    }

    public UserDTO updateUserRole(Long id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        user.setRole(role);
        return convertToDTO(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Finds the backend user for the given Supabase JWT claims, creating one if
     * it doesn't exist yet (lazy provisioning on first authenticated request).
     *
     * Priority:
     *  1. Match by supabaseId  — fastest path for returning users
     *  2. Match by email       — backfills supabaseId for pre-migration accounts
     *  3. Create new record    — first-ever login
     */
    public UserDTO findOrCreateFromJwt(String supabaseId, String email, String firstName, String lastName) {
        return userRepository.findBySupabaseId(supabaseId)
                .map(this::convertToDTO)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existing -> {
                            existing.setSupabaseId(supabaseId);
                            return convertToDTO(userRepository.save(existing));
                        })
                        .orElseGet(() -> {
                            User newUser = User.builder()
                                    .supabaseId(supabaseId)
                                    .email(email)
                                    .firstName(firstName != null ? firstName : "")
                                    .lastName(lastName != null ? lastName : "")
                                    .provider("supabase")
                                    .providerId(supabaseId)
                                    .build();
                            UserDTO created = convertToDTO(userRepository.save(newUser));
                            try {
                                sendWelcomeEmail(created, firstName);
                            } catch (Exception e) {
                                log.warn("Welcome email failed (user created): {}", e.getMessage());
                            }
                            return created;
                        })
                );
    }

    private void sendWelcomeEmail(UserDTO user, String firstName) {
        String name = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        SendNotificationRequest req = new SendNotificationRequest();
        req.setUserId(user.getId());
        req.setType(NotificationType.WELCOME);
        req.setChannel(NotificationChannel.EMAIL);
        req.setRecipientEmail(user.getEmail());
        req.setSendImmediately(true);
        req.setSubject("Welcome to Travolish!");
        req.setMessage("Hi " + name + ",\n\n"
                + "Your Travolish account is ready. Start exploring hotels and book your next stay.\n\n"
                + "Happy travels!");
        notificationService.sendNotificationAsync(req);
    }

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .preferredName(user.getPreferredName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .city(user.getCity())
                .timeZone(user.getTimeZone())
                .travelStyle(user.getTravelStyle())
                .bio(user.getBio())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .imageKey(user.getImageKey())
                .avatarUrl(user.getImageKey())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
