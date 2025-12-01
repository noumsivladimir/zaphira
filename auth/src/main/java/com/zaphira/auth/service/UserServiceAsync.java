package com.zaphira.auth.service;

import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.auth.event.UserEventPublisher;
import com.zaphira.auth.model.Role;
import com.zaphira.auth.model.User;
import com.zaphira.auth.repository.UserRepository;
import com.zaphira.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Version asynchrone de UserService utilisant Kafka pour créer le wallet.
 * Utilise UserEventPublisher pour publier un événement au lieu d'appeler directement wallet-service.
 */
@Slf4j
@Service("userServiceAsync")
@RequiredArgsConstructor
public class UserServiceAsync {

    private final UserRepository userRepository;
    private final UserEventPublisher eventPublisher;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Enregistre un utilisateur et publie un événement pour créer le wallet de manière asynchrone.
     */
    public User registerUser(RegisterRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(existing -> {
                    if (request.getFullName() != null
                            && !request.getFullName().isBlank()
                            && !request.getFullName().equals(existing.getFullName())) {
                        existing.setFullName(request.getFullName());
                        userRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> createNewUserWithAsyncWallet(request));
    }

    /**
     * Authentifie un utilisateur.
     */
    public User authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElse(null);
    }

    /**
     * Récupère un compte via email.
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * Crée un nouvel utilisateur et publie un événement pour créer le wallet de manière asynchrone.
     */
    private User createNewUserWithAsyncWallet(RegisterRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Generate default phoneNumber if not provided
        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            phoneNumber = "+237" + String.valueOf(Math.abs(request.getEmail().hashCode()))
                    .substring(0, Math.min(9, String.valueOf(Math.abs(request.getEmail().hashCode())).length()));
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(hashedPassword)
                .phoneNumber(phoneNumber)
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(newUser);

        // Publish event to Kafka for async wallet creation
        try {
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(savedUser.getId())
                    .email(savedUser.getEmail())
                    .fullName(savedUser.getFullName())
                    .phoneNumber(savedUser.getPhoneNumber())
                    .build();

            eventPublisher.publishUserRegistered(event);
            log.info("User {} registered, event published for wallet creation", savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to publish UserRegisteredEvent for user {}", savedUser.getId(), e);
            // User is already created, wallet will be created later via retry mechanism
        }

        return savedUser;
    }
}

