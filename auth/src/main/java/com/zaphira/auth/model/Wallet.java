package com.zaphira.auth.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

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

    @Column(nullable = false, unique = true, length = 8)
    private String walletNumber;

    @Column(nullable = false)
    private Double balance;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "senderWallet")
    private List<Transaction> sentTransactions;

    @OneToMany(mappedBy = "receiverWallet")
    private List<Transaction> receivedTransactions;

    private static final SecureRandom RANDOM = new SecureRandom();

    @PostPersist
    private void generateWalletNumber() {
        if (this.walletNumber == null) {
            this.walletNumber = generateSecureWalletNumber(this.id);
        }
    }

    private String generateSecureWalletNumber(Long id) {
        // Convertit l'id en chaîne et utilise le reste pour compléter 8 chiffres
        String idStr = String.valueOf(id);
        int remainingLength = 8 - idStr.length();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < remainingLength; i++) {
            sb.append(RANDOM.nextInt(10)); // chiffre aléatoire 0-9
        }
        sb.append(idStr);

        return sb.toString();
    }
}

