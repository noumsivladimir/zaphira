package com.zaphira.wallet.service;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.wallet.dto.WalletSummaryDTO;
import com.zaphira.wallet.dto.request.CreateSubWalletRequest;
import com.zaphira.wallet.dto.response.SubWalletResponse;

import java.util.List;

public interface WalletHierarchyService {

    // Création de sous-wallets
    SubWalletResponse createSubWallet(CreateSubWalletRequest request);

    // Navigation hiérarchique
    WalletDTO getWalletWithSubWallets(String walletNumber, boolean recursive);
    List<SubWalletResponse> getSubWallets(String walletNumber);
    List<WalletDTO> getParentChain(String walletNumber);


    List<WalletSummaryDTO> managingWallet(Long subWalletId);
//    WalletHierarchyDTO getFullHierarchy(String rootWalletNumber);

    // Déplacement
    void moveSubWallet(String subWalletNumber, String newParentWalletNumber);
    void detachSubWallet(String subWalletNumber); // Devient wallet racine

    // Soldes agrégés
//    BalanceSummaryDTO getAggregatedBalance(String walletNumber, boolean includeSubWallets);

    // Validation
    boolean canCreateSubWallet(String parentWalletNumber, Long requestingUserId);
    boolean isAncestorOf(String ancestorWalletNumber, String descendantWalletNumber);
}