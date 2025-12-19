package com.zaphira.wallet.service;

import com.zaphira.wallet.dto.request.CreateWalletRequest;
import com.zaphira.wallet.dto.response.CreateWalletResponse;
import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.models.enums.WalletStatus;
import com.zaphira.wallet.models.enums.WalletType;
import com.zaphira.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Slf4j
@RequiredArgsConstructor
@Service
public class WalletServiceImpl implements WalletService{

    private final WalletRepository walletRepository;
//    private final WalletMapper walletMapper;
   // private final UserService userServiceClient;


    @Override
    public CreateWalletResponse createWalletForUser(CreateWalletRequest request) {

        Long userId = request.getUserId();

        log.info("Creating wallet for user: {}", request.getUserId());

        //Algo generation du WalletNumber Unique
        String walletNumber = String.format("%08d", (userId * 1234567) % 100_000_000);

        log.info("WalletNumber generated: {}", walletNumber);
        // Créer le wallet avec walletNumber déjà défini

        Wallet wallet = Wallet.builder()
                .userId(request.getUserId())
                .availableBalance( BigDecimal.ZERO)
                .walletNumber(walletNumber)
                .type(WalletType.USER )
                .status(WalletStatus.ACTIVE)
                 // si ta colonne NOT NULL
                .build();


        // Sauvegarder le wallet en base
        Wallet saved = walletRepository.save(wallet);

        return toDTO(saved);
    }

//    @Override
//    public WalletDTO createWalletForMerchant(CreateWalletRequest request) {
//        return null;
//    }


//
//    @Override
//    public WalletDTO createWalletForUser(Long userId) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO getWalletByNumber(String walletNumber) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO getWalletById(Long id) {
//        return null;
//    }
//
//    @Override
//    public List<WalletDTO> getUserWallets(Long userId) {
//        return List.of();
//    }
//
//    @Override
//    public WalletDTO getPrimaryWallet(Long userId) {
//        return null;
//    }
//
//    @Override
//    public WalletSummaryDTO getWalletSummary(Long userId) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO freezeWallet(String walletNumber, FreezeWalletRequest request) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO unfreezeWallet(String walletNumber, String unfrozenBy, String notes) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO suspendWallet(String walletNumber, String reason, String suspendedBy) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO activateWallet(String walletNumber, String activatedBy) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO closeWallet(String walletNumber, String closedBy, String reason) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO creditWallet(String walletNumber, BalanceOperationRequest request) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO debitWallet(String walletNumber, BalanceOperationRequest request) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO blockAmount(String walletNumber, BalanceOperationRequest request) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO unblockAmount(String walletNumber, BalanceOperationRequest request) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO releaseBlockedAmount(String walletNumber, BalanceOperationRequest request) {
//        return null;
//    }
//
//    @Override
//    public TransactionValidationResponse validateTransaction(TransactionValidationRequest request) {
//        return null;
//    }
//
//    @Override
//    public void resetDailyLimits() {
//
//    }
//
//    @Override
//    public void resetMonthlyLimits() {
//
//    }
//
//    @Override
//    public WalletDTO updateLimits(String walletNumber, BigDecimal dailyLimit, BigDecimal monthlyLimit) {
//        return null;
//    }
//
//    @Override
//    public boolean hasAvailableBalance(String walletNumber, BigDecimal amount) {
//        return false;
//    }
//
//    @Override
//    public void recalculateBalance(String walletNumber) {
//
//    }
//
//    @Override
//    public WalletDTO createWallet(CreateWalletRequest request, Long requestingUserId) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO getWallet(String walletNumber, Long requestingUserId) {
//        return null;
//    }
//
//    @Override
//    public List<WalletDTO> getAccessibleWallets(Long userId) {
//        return List.of();
//    }
//
//    @Override
//    public WalletDTO freezeWallet(String walletNumber, FreezeWalletRequest request, Long requestingUserId) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO unfreezeWallet(String walletNumber, String unfrozenBy, String notes, Long requestingUserId) {
//        return null;
//    }
//
//    @Override
//    public WalletDTO transferBetweenSubWallets(TransferBetweenSubWalletsRequest request, Long requestingUserId) {
//        return null;
//    }
//
//    @Override
//    public BalanceSummaryDTO getConsolidatedBalance(String walletNumber, Long requestingUserId) {
//        return null;
//    }

    // Méthodes privées utilitaires

//    private void validateUserStatus(Long userId) {
//        // Appel au User Service pour vérifier le statut
//        // Cette méthode sera implémentée via Feign Client
//        try {
//            userServiceClient.getUserStatus(userId);
//        } catch (Exception e) {
//            log.error("Failed to validate user status for userId: {}", userId, e);
//            throw new InvalidOperationException("Impossible de valider le statut de l'utilisateur");
//        }
//    }

    private CreateWalletResponse toDTO(Wallet wallet) {
        return CreateWalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .walletNumber(wallet.getWalletNumber())
                .type(wallet.getType())
                .status(wallet.getStatus())
                .build();
    }
}

