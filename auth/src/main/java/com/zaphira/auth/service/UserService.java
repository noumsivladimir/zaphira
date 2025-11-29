package com.zaphira.auth.service;

import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.auth.model.Role;
import com.zaphira.auth.model.User;
import com.zaphira.auth.model.Wallet;
import com.zaphira.auth.repository.UserRepository;
import com.zaphira.auth.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Enregistre un utilisateur.
     * - Évite les doublons
     * - Met à jour le nom si besoin
     * - Crée un wallet si utilisateur nouveau
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
     * Méthode centralisée pour création user + wallet.
     */
    private User createNewUserWithWallet(RegisterRequest request) {

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(hashedPassword)
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(newUser);

        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(0.0)
                .build();

        walletRepository.save(wallet);

        return savedUser;
    }
}
