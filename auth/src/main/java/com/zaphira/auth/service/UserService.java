package com.zaphira.auth.service;

import com.zaphira.auth.client.WalletServiceClient;
import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.auth.model.Role;
import com.zaphira.auth.model.User;
import com.zaphira.common.dto.WalletDTO;
import com.zaphira.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletServiceClient walletServiceClient;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Inscription d'un utilisateur et création de son wallet.
     * Si l'utilisateur existe déjà, tente de créer ou récupérer le wallet.
     * @param request données d'inscription
     * @return User complet avec walletId si disponible
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
                    // Créer le wallet si absent
                    if (existing.getWalletId() == null) {
                        WalletDTO wallet = createWalletForUser(existing.getId());
                        if (wallet != null) {
                            existing.setWalletId(wallet.getId());
                            userRepository.save(existing);
                        }
                    }
                    return existing;
                })
                .orElseGet(() -> createNewUserWithWallet(request));
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
     * Crée un nouvel utilisateur et son wallet.
     */
    private User createNewUserWithWallet(RegisterRequest request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());

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
        log.info("User created successfully: {}", savedUser.getId());

        // Création du wallet via wallet-service
        WalletDTO wallet = createWalletForUser(savedUser.getId());
        if (wallet != null) {
            savedUser.setWalletId(wallet.getId());
            userRepository.save(savedUser);
            log.info("User updated with walletId: {}", wallet.getId());
        } else {
            log.warn("Wallet creation failed for user {}", savedUser.getId());
        }

        return savedUser;
    }

    /**
     * Crée un wallet pour un utilisateur via wallet-service.
     */
    private WalletDTO createWalletForUser(Long userId) {
        try {
            WalletDTO wallet = walletServiceClient.createWallet(userId, "XOF");
            log.info("✅ Wallet created successfully for user {}: {}", userId, wallet.getWalletNumber());
            return wallet;
        } catch (Exception e) {
            log.error("❌ Error creating wallet for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
