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

    // ✅ Register using RegisterRequest DTO (idempotent)
    public User registerUser(RegisterRequest request) {

        return userRepository.findByEmail(request.getEmail())
                .map(existing -> {
                    // Optionally refresh full name if it changed
                    if (request.getFullName() != null && !request.getFullName().isBlank()
                            && !request.getFullName().equals(existing.getFullName())) {
                        existing.setFullName(request.getFullName());
                        userRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    // Hash password
                    String hashedPassword = passwordEncoder.encode(request.getPassword());

                    // Create user
                    User newUser = User.builder()
                            .email(request.getEmail())
                            .fullName(request.getFullName())
                            .password(hashedPassword)
                            .role(Role.USER)
                            .build();

                    User savedUser = userRepository.save(newUser);

                    // Create wallet for new user
                    Wallet wallet = new Wallet();
                    wallet.setUser(savedUser);
                    wallet.setBalance(0.0);
                    walletRepository.save(wallet);

                    return savedUser;
                });
    }

    // ✅ Authenticate user
    public User authenticate(String email, String rawPassword) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
