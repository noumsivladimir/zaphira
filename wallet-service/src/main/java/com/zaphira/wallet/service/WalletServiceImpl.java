package com.zaphira.wallet.service;

import com.zaphira.common.dto.WalletSummaryDTO;
import com.zaphira.common.model.enums.Currency;
import com.zaphira.wallet.dto.WalletDTO;
import com.zaphira.wallet.dto.TransferRequest;
import com.zaphira.wallet.dto.request.*;
import com.zaphira.wallet.dto.response.CreateWalletResponse;
import com.zaphira.wallet.dto.response.TransactionValidationResponse;
import com.zaphira.wallet.exception.*;
import com.zaphira.wallet.mapper.WalletMapper;
import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.models.entities.WalletStatusHistory;
import com.zaphira.wallet.models.enums.WalletStatus;
import com.zaphira.wallet.models.enums.WalletType;
import com.zaphira.wallet.repository.WalletRepository;

import com.zaphira.wallet.security.AuthenticatedUser;
import com.zaphira.wallet.exception.UnauthorizedWalletAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;



@Slf4j
@RequiredArgsConstructor
@Service
public class WalletServiceImpl implements WalletService{

    private final WalletRepository walletRepository;
    private final WalletQueryService walletQueryService;
    private final WalletMapper walletMapper;
    
    @Lazy
    private final WalletHierarchyService walletHierarchyService;
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

    @Override
    public CreateWalletResponse createWalletForMerchant(CreateMerchantWalletRequest request) {


        Wallet wallet = walletRepository.findByWalletNumber(request.getWalletNumber()).orElseThrow(
                () -> new WalletNotFoundException(request.getWalletNumber()));

        log.info ("Verifying if a merchant wallet exists for wallet: {}", request.getWalletNumber());
        if (wallet.getMerchantCode() != null && wallet.getMerchantName() != null) {
            throw new MerchantAlreadyExistsException("ALREADY MERCHANT USER");
        }

        log.info("Creating Merchant Wallet for WalletId: {}", wallet.getId());

        //Algo generation du WalletNumber Unique
        String merchantCode = String.format("%06d", (wallet.getId() * 1234567) % 1_000_000);

        log.info("WalletCode generated: {}", merchantCode);
        // Créer le wallet avec walletNumber déjà défini

        wallet.setMerchantName(request.getMerchantName());
        wallet.setMerchantCode(merchantCode);
        Wallet saved = walletRepository.save(wallet);

        return toDTO(saved);
    }


//
//    @Override
//    public WalletDTO createWalletForUser(Long userId) {
//        return null;
//    }
//
    @Override
    public WalletDTO getWalletByNumber(String walletNumber) {


        Wallet wallet = walletQueryService.findWalletByNumber(walletNumber);

        return walletMapper.toDTO(wallet);
    }

    @Override
//    @Transactional(readOnly = true)
    @Transactional
    public WalletDTO getWalletById(Long id) {
        Wallet wallet = walletRepository.findByWalletIdWithLock(id)
                .orElseThrow(() -> new WalletNotFoundException("Wallet non trouvé avec l'ID: " + id));
        return walletMapper.toDTO(wallet);
    }



    @Override
    public WalletSummaryDTO getWalletSummary(Long walletId) {

        Wallet wallet = walletRepository.findByWalletIdWithLock(walletId).orElseThrow(
                () -> new WalletNotFoundException("Wallet not found with id: " + walletId)
        );

        //+1 for the main wallet
        int totalWallets = walletHierarchyService.managingWallet(wallet.getId()).size() + 1;


//        List<Wallet> wallets = walletRepository.findByUserId(userId);
//
//        BigDecimal totalBalance = wallets.stream()
//                .filter(w -> !WalletStatus.CLOSED.equals(w.getStatus()))
//                .map(Wallet::getTotalBalance)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return WalletSummaryDTO.builder()
                .walletId(walletId)
                .totalBalanceAllWallets(BigDecimal.ZERO)
                .totalWallets(totalWallets)
                .currency(Currency.XAF)
            .merchant(wallet.getMerchantCode() != null)
                .build();
    }

    @Override
    public WalletSummaryDTO getWalletSummaryByWalletNumber(String walletNumber) {
        Wallet wallet = walletQueryService.findWalletByNumber(walletNumber);
        return WalletSummaryDTO.builder()
                .walletNumber(walletNumber)
            .userId(wallet.getUserId())
            .walletId(wallet.getId())
                .currency(Currency.XAF)
            .merchant(wallet.getMerchantCode() != null)
                .build();
    }

    @Override
    public WalletDTO freezeWallet(String walletNumber, FreezeWalletRequest request) {

        log.info("Freezing wallet: {}", walletNumber);

        AuthenticatedUser user = requireUser();

        Wallet wallet = walletQueryService.findWalletByNumberWithLock(walletNumber);
        enforceWalletOwnership(wallet, user);

        if (WalletStatus.FROZEN.equals(wallet.getStatus())) {
            throw new InvalidOperationException("Le wallet est déjà gelé");
        }

        if (WalletStatus.CLOSED.equals(wallet.getStatus())) {
            throw new InvalidOperationException("Impossible de geler un wallet fermé");
        }

        WalletStatus previousStatus = wallet.getStatus();
        wallet.setStatus(WalletStatus.FROZEN);
        wallet.setFrozenReason(request.getReason());
        wallet.setFrozenAt(LocalDateTime.now());
        wallet.setFrozenBy(request.getFrozenBy());

        // Ajouter à l'historique
        WalletStatusHistory history = WalletStatusHistory.builder()
                .previousStatus(previousStatus)
                .newStatus(WalletStatus.FROZEN)
                .reason(request.getReason())
                .changedBy(request.getFrozenBy())
                .notes(request.getNotes())
                .build();
        wallet.addStatusHistory(history);

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet frozen successfully: {}", walletNumber);

        return walletMapper.toDTO(savedWallet);
    }

    @Override
    public WalletDTO unfreezeWallet(String walletNumber, String unfrozenBy, String notes) {
        log.info("Unfreezing wallet: {}", walletNumber);

        AuthenticatedUser user = requireUser();

        Wallet wallet = walletQueryService.findWalletByNumberWithLock(walletNumber);
        enforceWalletOwnership(wallet, user);

        if (!WalletStatus.FROZEN.equals(wallet.getStatus())) {
            throw new InvalidOperationException("Le wallet n'est pas gelé");
        }

        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setFrozenReason(null);
        wallet.setFrozenAt(null);
        wallet.setFrozenBy(null);

        // Ajouter à l'historique
        WalletStatusHistory history = WalletStatusHistory.builder()
                .previousStatus(WalletStatus.FROZEN)
                .newStatus(WalletStatus.ACTIVE)
                .reason("Wallet dégelé")
                .changedBy(unfrozenBy)
                .notes(notes)
                .build();
        wallet.addStatusHistory(history);

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet unfrozen successfully: {}", walletNumber);

        return walletMapper.toDTO(savedWallet);
    }

    @Override
    public WalletDTO suspendWallet(String walletNumber, String reason, String suspendedBy) {
        log.info("Suspending wallet: {}", walletNumber);

        AuthenticatedUser user = requireUser();

        Wallet wallet = walletQueryService.findWalletByNumberWithLock(walletNumber);
        enforceWalletOwnership(wallet, user);

        if (WalletStatus.CLOSED.equals(wallet.getStatus())) {
            throw new InvalidOperationException("Impossible de suspendre un wallet fermé");
        }

        WalletStatus previousStatus = wallet.getStatus();
        wallet.setStatus(WalletStatus.SUSPENDED);

        WalletStatusHistory history = WalletStatusHistory.builder()
                .previousStatus(previousStatus)
                .newStatus(WalletStatus.SUSPENDED)
                .reason(reason)
                .changedBy(suspendedBy)
                .build();
        wallet.addStatusHistory(history);

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet suspended successfully: {}", walletNumber);

        return walletMapper.toDTO(savedWallet);
    }

    @Override
    public WalletDTO activateWallet(String walletNumber, String activatedBy) {
        log.info("Activating wallet: {}", walletNumber);

        AuthenticatedUser user = requireUser();

        Wallet wallet = walletQueryService.findWalletByNumberWithLock(walletNumber);
        enforceWalletOwnership(wallet, user);

        if (WalletStatus.CLOSED.equals(wallet.getStatus())) {
            throw new InvalidOperationException("Impossible d'activer un wallet fermé");
        }

        WalletStatus previousStatus = wallet.getStatus();
        wallet.setStatus(WalletStatus.ACTIVE);

        WalletStatusHistory history = WalletStatusHistory.builder()
                .previousStatus(previousStatus)
                .newStatus(WalletStatus.ACTIVE)
                .reason("Wallet activé")
                .changedBy(activatedBy)
                .build();
        wallet.addStatusHistory(history);

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet activated successfully: {}", walletNumber);

        return walletMapper.toDTO(savedWallet);
    }

    @Override

    public WalletDTO closeWallet(String walletNumber, String closedBy, String reason) {
        log.info("Closing wallet: {}", walletNumber);

        AuthenticatedUser user = requireUser();

        Wallet wallet = walletQueryService.findWalletByNumberWithLock(walletNumber);
        enforceWalletOwnership(wallet, user);

        // Vérifier que le solde est à zéro
        if (wallet.getTotalBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidOperationException("Impossible de fermer un wallet avec un solde non nul");
        }

        WalletStatus previousStatus = wallet.getStatus();
        wallet.setStatus(WalletStatus.CLOSED);
        wallet.setClosedAt(LocalDateTime.now());

        WalletStatusHistory history = WalletStatusHistory.builder()
                .previousStatus(previousStatus)
                .newStatus(WalletStatus.CLOSED)
                .reason(reason)
                .changedBy(closedBy)
                .build();
        wallet.addStatusHistory(history);

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet closed successfully: {}", walletNumber);

        return walletMapper.toDTO(savedWallet);
    }

    @Override
    @Transactional
    public WalletDTO creditWallet(Long walletId, BalanceOperationRequest request) {
        log.info("Crediting wallet: {} with amount: {}", walletId, request.getAmount());
        AuthenticatedUser user = requireUser();

        Wallet wallet = walletRepository.findByWalletIdWithLock(walletId).orElseThrow(
            () -> new WalletNotFoundException("Wallet not found with id: " + walletId)
        );
        enforceWalletOwnership(wallet, user);
        log.info("Crediting wallet number : {} with amount: {}", wallet.getWalletNumber(), request.getAmount());

        // Vérifier que le wallet peut recevoir des fonds
        if (!wallet.getStatus().canReceive()) {
            throw new WalletInactiveException("Le wallet ne peut pas recevoir de fonds. Statut: " + wallet.getStatus());
        }

        wallet.credit(request.getAmount());

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet credited successfully. New balance: {}", savedWallet.getAvailableBalance());

        return walletMapper.toDTO(savedWallet);
    }




    @Override
    @Transactional
    public WalletDTO debitWallet(Long walletId, BalanceOperationRequest request) {
        log.info("Debiting wallet: {} with amount: {}", walletId, request.getAmount());

        AuthenticatedUser user = requireUser();

        Wallet wallet = walletRepository.findByWalletIdWithLock(walletId).orElseThrow(
                () -> new WalletNotFoundException("Wallet not found with id: " + walletId)
        );
        enforceWalletOwnership(wallet, user);

        // Vérifications
        if (!wallet.canTransact()) {
            throw new WalletInactiveException("Le wallet ne peut pas effectuer de transactions. Statut: " + wallet.getStatus());
        }

        if (!wallet.hasAvailableBalance(request.getAmount())) {
            throw new InsufficientBalanceException("Solde insuffisant. Disponible: " + wallet.getAvailableBalance());
        }

        // Vérifier les limites
        checkLimits(wallet, request.getAmount());

        wallet.debit(request.getAmount());

        // Mettre à jour les montants dépensés
        wallet.setDailySpent(wallet.getDailySpent().add(request.getAmount()));
        wallet.setMonthlySpent(wallet.getMonthlySpent().add(request.getAmount()));

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet debited successfully. New balance: {}", savedWallet.getAvailableBalance());

        return walletMapper.toDTO(savedWallet);
    }

    @Override
    @Transactional
    public WalletDTO blockAmount(Long walletId, BalanceOperationRequest request) {
        log.info("Blocking amount in wallet: {} amount: {}", walletId, request.getAmount());

        try {
            AuthenticatedUser user = requireUser();
            Wallet wallet = walletRepository.findByWalletIdWithLock(walletId).orElseThrow(
                    () -> new WalletNotFoundException("Wallet not found with id: " + walletId)
            );
            enforceWalletOwnership(wallet, user);

            if (!wallet.canTransact()) {
                throw new WalletInactiveException("Le wallet ne peut pas effectuer de transactions");
            }

            log.debug("Before block - Available: {}, Blocked: {}, Total: {}",
                    wallet.getAvailableBalance(), wallet.getBlockedBalance(), wallet.getTotalBalance());

            wallet.blockAmount(request.getAmount());

            log.debug("After block - Available: {}, Blocked: {}, Total: {}",
                    wallet.getAvailableBalance(), wallet.getBlockedBalance(), wallet.getTotalBalance());

            Wallet savedWallet = walletRepository.save(wallet);

            log.info("Amount blocked successfully. Available: {}, Blocked: {}",
                    savedWallet.getAvailableBalance(), savedWallet.getBlockedBalance());

            return walletMapper.toDTO(savedWallet);
        } catch (Exception e) {
            log.error("Error blocking amount: ", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public WalletDTO unblockAmount(Long walletId, BalanceOperationRequest request) {
        log.info("Unblocking amount in wallet: {} amount: {}", walletId, request.getAmount());

        AuthenticatedUser user = requireUser();
        Wallet wallet = walletRepository.findByWalletIdWithLock(walletId).orElseThrow(
                () -> new WalletNotFoundException("Wallet not found with id: " + walletId)
        );
        enforceWalletOwnership(wallet, user);

        wallet.unblockAmount(request.getAmount());

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Amount unblocked successfully. Available: {}, Blocked: {}",
                savedWallet.getAvailableBalance(), savedWallet.getBlockedBalance());

        return walletMapper.toDTO(savedWallet);
    }

    @Override
    @Transactional
    public WalletDTO releaseBlockedAmount(Long  walletId, BalanceOperationRequest request) {
        log.info("Releasing blocked amount from wallet: {} amount: {}", walletId, request.getAmount());

        AuthenticatedUser user = requireUser();
        Wallet wallet = walletRepository.findByWalletIdWithLock(walletId).orElseThrow(
                () -> new WalletNotFoundException("Wallet not found with id: " + walletId)
        );
        enforceWalletOwnership(wallet, user);
        wallet.releaseBlockedAmount(request.getAmount());

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Blocked amount released successfully. Available: {}, Blocked: {}",
                savedWallet.getAvailableBalance(), savedWallet.getBlockedBalance());

        return walletMapper.toDTO(savedWallet);
    }

    @Override
    @Transactional
    public void transfer(TransferRequest request) {
        log.info("Transferring from {} to {} amount {}", request.getSenderWalletNumber(), request.getReceiverWalletNumber(), request.getAmount());

        AuthenticatedUser user = requireUser();

        if (request.getSenderWalletNumber().equals(request.getReceiverWalletNumber())) {
            throw new InvalidOperationException("Sender and receiver wallets must differ");
        }

        Wallet sender = walletQueryService.findWalletByNumberWithLock(request.getSenderWalletNumber());
        Wallet receiver = walletQueryService.findWalletByNumberWithLock(request.getReceiverWalletNumber());

        if (!hasRole(user, "ADMIN")) {
            enforceWalletOwnership(sender, user);
        }

        if (!sender.canTransact()) {
            throw new WalletInactiveException("Sender wallet cannot transact. Status: " + sender.getStatus());
        }
        if (!receiver.getStatus().canReceive()) {
            throw new WalletInactiveException("Receiver wallet cannot receive funds. Status: " + receiver.getStatus());
        }

        if (!sender.hasAvailableBalance(request.getAmount())) {
            throw new InsufficientBalanceException("Solde insuffisant. Disponible: " + sender.getAvailableBalance());
        }

        checkLimits(sender, request.getAmount());

        sender.debit(request.getAmount());
        sender.setDailySpent(sender.getDailySpent().add(request.getAmount()));
        sender.setMonthlySpent(sender.getMonthlySpent().add(request.getAmount()));

        receiver.credit(request.getAmount());

        walletRepository.save(sender);
        walletRepository.save(receiver);

        log.info("Transfer completed. Sender balance: {}, Receiver balance: {}", sender.getAvailableBalance(), receiver.getAvailableBalance());
    }

    private AuthenticatedUser requireUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser au) {
            return au;
        }
        throw new UnauthorizedWalletAccessException("Unauthenticated request");
    }

    private boolean hasRole(AuthenticatedUser user, String role) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(r -> r.equalsIgnoreCase(role) || r.equalsIgnoreCase("ROLE_" + role));
    }

    private void enforceWalletOwnership(Wallet wallet, AuthenticatedUser user) {
        if (wallet.getUserId() == null) {
            throw new UnauthorizedWalletAccessException("Wallet ownership cannot be verified");
        }
        if (hasRole(user, "ADMIN")) {
            return;
        }
        if (!wallet.getUserId().equals(user.getId())) {
            throw new UnauthorizedWalletAccessException("Forbidden: wallet not owned by requester");
        }
    }

    @Override
    public TransactionValidationResponse validateTransaction(TransactionValidationRequest request) {
        log.info("Validating transaction for wallet: {}", request.getWalletNumber());

        try {
            Wallet wallet = walletQueryService.findWalletByNumber(request.getWalletNumber());

            // 1. Vérifier que le wallet est actif
            if (!wallet.canTransact()) {
                return TransactionValidationResponse.invalid(
                        "Wallet inactif. Statut: " + wallet.getStatus(),
                        "WALLET_INACTIVE"
                );
            }

            // 2. Pour les débits, vérifier le solde
            if ("DEBIT".equalsIgnoreCase(request.getTransactionType())) {
                if (!wallet.hasAvailableBalance(request.getAmount())) {
                    return TransactionValidationResponse.invalid(
                            "Solde insuffisant",
                            "INSUFFICIENT_BALANCE"
                    );
                }

                // 3. Vérifier les limites
                if (!checkLimitsValid(wallet, request.getAmount())) {
                    return TransactionValidationResponse.invalid(
                            "Limite dépassée",
                            "LIMIT_EXCEEDED"
                    );
                }
            }

            // 4. Vérifier la devise (si applicable)
            // ... logique additionnelle si nécessaire

            return TransactionValidationResponse.valid();

        } catch (WalletNotFoundException e) {
            return TransactionValidationResponse.invalid(
                    "Wallet non trouvé",
                    "WALLET_NOT_FOUND"
            );
        }
    }

    @Override
    public void resetDailyLimits() {

        log.info("Resetting daily limits for all wallets");

        List<Wallet> wallets = walletRepository.findAllByStatus(WalletStatus.ACTIVE);
        LocalDate today = LocalDate.now();

        wallets.forEach(wallet -> {
            LocalDate lastReset = wallet.getLastLimitReset() != null ?
                    wallet.getLastLimitReset().toLocalDate() : null;

            if (lastReset == null || !lastReset.equals(today)) {
                wallet.setDailySpent(BigDecimal.ZERO);
                wallet.setLastLimitReset(LocalDateTime.now());
            }
        });

        walletRepository.saveAll(wallets);
        log.info("Daily limits reset for {} wallets", wallets.size());
    }

    @Override
    public void resetMonthlyLimits() {

        log.info("Resetting monthly limits for all wallets");

        List<Wallet> wallets = walletRepository.findAllByStatus(WalletStatus.ACTIVE);

        wallets.forEach(wallet -> wallet.setMonthlySpent(BigDecimal.ZERO));

        walletRepository.saveAll(wallets);
        log.info("Monthly limits reset for {} wallets", wallets.size());
    }

    @Override
    public WalletDTO updateLimits(String walletNumber, BigDecimal dailyLimit, BigDecimal monthlyLimit) {
        log.info("Updating limits for wallet: {}", walletNumber);

        AuthenticatedUser user = requireUser();

        Wallet wallet = walletQueryService.findWalletByNumberWithLock(walletNumber);
        enforceWalletOwnership(wallet, user);

        if (dailyLimit != null) {
            wallet.setDailyLimit(dailyLimit);
        }

        if (monthlyLimit != null) {
            wallet.setMonthlyLimit(monthlyLimit);
        }

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Limits updated successfully for wallet: {}", walletNumber);

        return walletMapper.toDTO(savedWallet);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean hasAvailableBalance(String walletNumber, BigDecimal amount) {
        AuthenticatedUser user = requireUser();
        Wallet wallet = walletQueryService.findWalletByNumber(walletNumber);
        enforceWalletOwnership(wallet, user);
        return wallet.hasAvailableBalance(amount);
    }

//    @Override
//    public Long getWalletIdByWalletNumber(String walletNumber) {
//
//        Wallet wallet = walletQueryService.findWalletByNumber(walletNumber);
//        log.info("Wallet id for wallet: {}", wallet.getId());
//        return wallet.getId();
//    }


    
    @Override
    public void recalculateBalance(String walletNumber) {

        log.info("Recalculating balance for wallet: {}", walletNumber);

        AuthenticatedUser user = requireUser();
        Wallet wallet = walletQueryService.findWalletByNumberWithLock(walletNumber);
        enforceWalletOwnership(wallet, user);
        wallet.calculateTotalBalance();

        walletRepository.save(wallet);
        log.info("Balance recalculated for wallet: {}", walletNumber);
    }



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
//
//     Méthodes privées utilitaires
//
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

        CreateWalletResponse.CreateWalletResponseBuilder builder = CreateWalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .walletNumber(wallet.getWalletNumber())
                .type(wallet.getType())
                .status(wallet.getStatus());

        // ajout conditionnel
        if (wallet.getMerchantCode() != null) {
            builder.merchantCode(wallet.getMerchantCode());
        }
        if (wallet.getMerchantName() != null) {
            builder.merchantName(wallet.getMerchantName());
        }

        return builder.build();


    }

    private void checkLimits(Wallet wallet, BigDecimal amount) {
        // Vérifier limite journalière
        if (wallet.getDailyLimit() != null) {
            BigDecimal newDailySpent = wallet.getDailySpent().add(amount);
            if (newDailySpent.compareTo(wallet.getDailyLimit()) > 0) {
                throw new LimitExceededException("Limite journalière dépassée");
            }
        }

        // Vérifier limite mensuelle
        if (wallet.getMonthlyLimit() != null) {
            BigDecimal newMonthlySpent = wallet.getMonthlySpent().add(amount);
            if (newMonthlySpent.compareTo(wallet.getMonthlyLimit()) > 0) {
                throw new LimitExceededException("Limite mensuelle dépassée");
            }
        }
    }

    private boolean checkLimitsValid(Wallet wallet, BigDecimal amount) {
        try {
            checkLimits(wallet, amount);
            return true;
        } catch (LimitExceededException e) {
            return false;
        }
    }


    //Utilities functions


  

}
