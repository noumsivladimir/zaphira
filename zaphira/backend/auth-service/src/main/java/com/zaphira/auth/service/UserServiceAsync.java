package com.zaphira.auth.service;

import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.auth.event.UserEventPublisher;
import com.zaphira.auth.repository.UserRepository;
import com.zaphira.common.event.UserRegisteredEvent;
import com.zaphira.common.model.entities.RegularUser;
import com.zaphira.common.model.entities.User;
import com.zaphira.common.model.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service("userServiceAsync")
@RequiredArgsConstructor
public class UserServiceAsync {

    private final UserRepository userRepository; // Gère com.zaphira.common.model.entities.User
    private final UserEventPublisher eventPublisher;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Enregistre un utilisateur et publie un événement pour créer le wallet de manière asynchrone.
     */
    public User registerUser(RegisterRequest request) {
        return userRepository.findByPhoneNumber(request.getPhoneNumber())
                .map(existing -> {
                    if (request.getFullName() != null
                            && !request.getFullName().isBlank()
                            && !request.getFullName().equals(existing.getFullName())) {
                        String[] names = request.getFullName().trim().split("\\s+", 2);
                        existing.setFirstName(names[0]);
                        existing.setLastName(names.length > 1 ? names[1] : "");
                        userRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> createNewUserWithAsyncWallet(request));
    }

    /**
     * Authentifie un utilisateur via phoneNumber + PIN.
     */
    public User authenticate(String phoneNumber, String rawPin) {
        if (phoneNumber == null || phoneNumber.isBlank() || rawPin == null || rawPin.isBlank()) {
            return null;
        }

        return userRepository.findByPhoneNumber(phoneNumber)
                .filter(user -> {
                    if (user.getPin() == null || user.getPin().isBlank()) {
                        log.warn("User {} has no PIN set", user.getUserId());
                        return false;
                    }
                    return passwordEncoder.matches(rawPin, user.getPin());
                })
                .orElse(null);
    }

    /**
     * Récupère un utilisateur via phoneNumber.
     */
    public User findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).orElse(null);
    }

    /**
     * Crée un nouvel utilisateur concret et publie un événement Kafka pour création asynchrone du wallet.
     */
    private User createNewUserWithAsyncWallet(RegisterRequest request) {
        String hashedPin = passwordEncoder.encode(request.getPin());

        String phoneNumber = request.getPhoneNumber();
        if (phoneNumber == null || phoneNumber.isBlank()) {
            phoneNumber = "+237" + String.valueOf(Math.abs(request.getEmail().hashCode()))
                    .substring(0, Math.min(9, String.valueOf(Math.abs(request.getEmail().hashCode())).length()));
        }

        String[] names = request.getFullName() != null ? request.getFullName().trim().split("\\s+", 2) : new String[]{"", ""};
        String firstName = names.length > 0 ? names[0] : "";
        String lastName = names.length > 1 ? names[1] : "";

        // Création d'un utilisateur concret (RegularUser)
        RegularUser newUser = new RegularUser();
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setPin(hashedPin);
        newUser.setPhoneNumber(phoneNumber);
        newUser.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        newUser.setEmail(request.getEmail());
        newUser.setWalletId("WALLET-" + System.currentTimeMillis());

        User savedUser = userRepository.save(newUser);

        // Publication de l'événement Kafka
        try {
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(savedUser.getUserId())
                    .phoneNumber(savedUser.getPhoneNumber())
                    .firstName(savedUser.getFirstName())
                    .lastName(savedUser.getLastName())
                    .build();

            eventPublisher.publishUserRegistered(event);
            log.info("User {} registered, event published for wallet creation", savedUser.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish UserRegisteredEvent for user {}", savedUser.getUserId(), e);
        }

        return savedUser;
    }
}
