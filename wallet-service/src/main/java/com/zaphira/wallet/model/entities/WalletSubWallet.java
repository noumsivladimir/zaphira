package com.zaphira.wallet.model.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_subwallet", indexes = {
        @Index(name = "idx_wallet_subwallet_wallet", columnList = "wallet_id"),
        @Index(name = "idx_wallet_subwallet_subwallet", columnList = "subwallet_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletSubWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;  // References Wallet.id (Long)

    @Column(name = "subwallet_id", nullable = false)
    private Long subwalletId;  // References SubWallet.id (UUID)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", insertable = false, updatable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subwallet_id", insertable = false, updatable = false)
    private SubWallet subWallet;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}