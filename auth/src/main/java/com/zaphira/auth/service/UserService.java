package com.zaphira.auth.service;

import com.zaphira.auth.client.WalletServiceClient;
import com.zaphira.auth.dto.RegisterRequest;
import com.zaphira.common.dto.WalletDTO;
import com.zaphira.common.model.entities.RegularUser;
import com.zaphira.common.model.entities.User;
import com.zaphira.common.model.enums.AccountStatus;
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
     */
  public User registerUser(RegisterRequest request) {
    return userRepository.findByPhoneNumber(request.getPhoneNumber())
            .map(existing -> {
                // Mise à jour du nom si besoin
                if (request.getFullName() != null && !request.getFullName().isBlank()
                        && !request.getFullName().equals(existing.getFullName())) {
                    String[] names = request.getFullName().trim().split("\\s+", 2);
                    existing.setFirstName(names[0]);
                    existing.setLastName(names.length > 1 ? names[1] : "");
                    userRepository.save(existing);
                }
                // Création du wallet si absent
                if (existing.getWalletId() == null) {
                    WalletDTO wallet = createWalletForUser(existing.getUserId());
                    if (wallet != null) {
                        existing.setWalletId(String.valueOf(wallet.getId())); // <-- conversion en String
                        userRepository.save(existing);
                    }
                }
                return existing;
            })
            .orElseGet(() -> createNewUserWithWallet(request));
}


    /**
     * Authentifie un utilisateur via phoneNumber + PIN.
     */
    public User authenticate(String phoneNumber, String pin) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .filter(user -> passwordEncoder.matches(pin, user.getPin()))
                .orElse(null);
    }

    /**
     * Crée un nouvel utilisateur et son wallet.
     */
    private User createNewUserWithWallet(RegisterRequest request) {
        String hashedPin = passwordEncoder.encode(request.getPin());

        // Séparation fullName en firstName et lastName
        String[] names = request.getFullName() != null ? request.getFullName().trim().split("\\s+", 2) : new String[]{"", ""};
        String firstName = names.length > 0 ? names[0] : "";
        String lastName = names.length > 1 ? names[1] : "";

        // Création de l'utilisateur concret : RegularUser
        RegularUser newUser = RegularUser.builder()
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(request.getPhoneNumber())
                .pin(hashedPin)
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .build();

        // Sauvegarde dans la base
        RegularUser savedUser = userRepository.save(newUser);
        log.info("User created successfully: {}", savedUser.getUserId());

        // Création du wallet via wallet-service
        WalletDTO wallet = createWalletForUser(savedUser.getUserId());
        if (wallet != null) {
            savedUser.setWalletId(String.valueOf(wallet.getId()));
            userRepository.save(savedUser);
            log.info("User updated with walletId: {}", wallet.getId());
        } else {
            log.warn("Wallet creation failed for user {}", savedUser.getUserId());
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
