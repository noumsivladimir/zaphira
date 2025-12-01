package com.zaphira.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.security.SecureRandom;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_number", nullable = false, length = 8, unique = true)
    private String walletNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // Reference to user in auth-service (no JPA relation)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    private static final SecureRandom RANDOM = new SecureRandom();

    @PostPersist
    private void generateWalletNumber() {
        if (this.walletNumber == null) {
            this.walletNumber = generateSecureWalletNumber(this.id);
        }
    }

    private String generateSecureWalletNumber(Long id) {
        String idStr = String.valueOf(id);
        int remainingLength = 8 - idStr.length();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < remainingLength; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        sb.append(idStr);

        return sb.toString();
    }
}

