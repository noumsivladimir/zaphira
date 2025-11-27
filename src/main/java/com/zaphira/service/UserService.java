package com.zaphira.zaphira;

import com.zaphira.zaphira.RegisterRequest;
import com.zaphira.zaphira.Role;
import com.zaphira.zaphira.User;
import com.zaphira.zaphira.Wallet;
import com.zaphira.zaphira.UserRepository;
import com.zaphira.zaphira.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ✅ Register using RegisterRequest DTO
    public User registerUser(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already used");
        }

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
