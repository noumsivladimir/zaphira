package com.zaphira.wallet.service;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.common.model.enums.Currency;
import com.zaphira.wallet.dto.WalletSummaryDTO;
import com.zaphira.wallet.dto.request.CreateSubWalletRequest;
import com.zaphira.wallet.dto.response.SubWalletResponse;
import com.zaphira.wallet.mapper.SubWalletMapper;
import com.zaphira.wallet.models.entities.SubWallet;
import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.models.entities.WalletSubWallet;
import com.zaphira.wallet.models.enums.WalletStatus;
import com.zaphira.wallet.repository.SubWalletRepository;
import com.zaphira.wallet.repository.WalletSubWalletRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class WalletHierarchyServiceImpl implements WalletHierarchyService{

    private final SubWalletRepository subWalletRepository;
    private final WalletQueryService walletQueryService;
    private final WalletSubWalletRepository walletSubWalletRepository;
    private final SubWalletMapper subWalletMapper;

    @Override
    public SubWalletResponse createSubWallet(CreateSubWalletRequest request) {


        // Step 1: Create SubWallet with all required fields explicitly set
        SubWallet subWallet = SubWallet.builder()
                .subWalletName(request.getSubWalletName())
                .type(request.getType())
                .currency(Currency.XAF)  // Explicitly set
                .status(WalletStatus.ACTIVE)  // Explicitly set
                .availableBalance(BigDecimal.ZERO)  // Explicitly set
                .blockedBalance(BigDecimal.ZERO)  // Explicitly set
                .totalBalance(BigDecimal.ZERO)  // Explicitly set
                .isDefault(false)  // Explicitly set
                .build();

        log.info("Creating subwallet: {}", subWallet.getSubWalletName());


        //save wallet to Repo
        SubWallet createdSubWallet = subWalletRepository.save(subWallet);

        log.info("SubWallet with ID {}", subWallet.getId() + "Created successfully for user:");

        // Step 3: Create relationships for each managing wallet
        for (String walletNumber : request.getWalletNumberWhichCanManage()) {
            Wallet wallet = walletQueryService.findWalletByNumber(walletNumber);
            WalletSubWallet link = WalletSubWallet.builder()
                    .walletId(wallet.getId())
                    .subwalletId(createdSubWallet.getId())
                    .subWallet(createdSubWallet)
                    .build();
            walletSubWalletRepository.save(link);

        }

        SubWalletResponse subWalletResponse = subWalletMapper.toResponse(createdSubWallet, managingWallet(createdSubWallet.getId()) );

        // ✅ Step 3: Reload the SubWallet with relationships
        return subWalletResponse;

    }

    @Override
    public WalletDTO getWalletWithSubWallets(String walletNumber, boolean recursive) {
        return null;
    }

    @Override
    public List<SubWalletResponse> getSubWallets(String walletNumber) {


        Wallet wallet = walletQueryService.findWalletByNumber(walletNumber);

        Long walletId = wallet.getId();

        List <WalletSubWallet > subWalletList = walletSubWalletRepository.findByWalletId(walletId);

        List <SubWalletResponse > subWalletResponses = new ArrayList<>();

        for (WalletSubWallet subWallet : subWalletList) {

            Long subwalletId = subWallet.getSubwalletId();
            SubWallet subWalletEntity = subWalletRepository.findById(subwalletId)
                    .orElseThrow(() -> new IllegalArgumentException("SubWallet with ID " + subwalletId + " does not exist"));

            subWalletResponses.add(subWalletMapper.toResponse(subWalletEntity, managingWallet(subwalletId)));

        }

        return subWalletResponses;


    }

    @Override
    public List<SubWalletResponse> getSubWalletsByWalletId(Long walletId) {
        List<WalletSubWallet> subWalletList = walletSubWalletRepository.findByWalletId(walletId);
        List<SubWalletResponse> subWalletResponses = new ArrayList<>();

        for (WalletSubWallet subWallet : subWalletList) {
            Long subwalletId = subWallet.getSubwalletId();
            SubWallet subWalletEntity = subWalletRepository.findById(subwalletId)
                    .orElseThrow(() -> new IllegalArgumentException("SubWallet with ID " + subwalletId + " does not exist"));

            subWalletResponses.add(subWalletMapper.toResponse(subWalletEntity, managingWallet(subwalletId)));
        }

        return subWalletResponses;
    }

    @Override
    public List<WalletDTO> getParentChain(String walletNumber) {
        return List.of();
    }

    @Override
    public List<WalletSummaryDTO> managingWallet(Long subWalletId) {

        List <Long> walletId = walletSubWalletRepository.findAllWalletIdBySuWalletNumber(subWalletId);

        List <Wallet > wallets = walletQueryService.findAllById(walletId);

//        List <Wallet > wallets = walletRepository.findAllById(walletId);

        List <WalletSummaryDTO > managingWallets;

        managingWallets = wallets.stream()
                .map(wallet -> WalletSummaryDTO.builder()
                        .userId(wallet.getUserId())
                        .walletNumber(wallet.getWalletNumber())
                        .build()
                )
                .toList();

        return managingWallets;
    }

    @Override
    public void moveSubWallet(String subWalletNumber, String newParentWalletNumber) {

    }

    @Override
    public void detachSubWallet(String subWalletNumber) {

    }

    @Override
    public boolean canCreateSubWallet(String parentWalletNumber, Long requestingUserId) {
        return false;
    }

    @Override
    public boolean isAncestorOf(String ancestorWalletNumber, String descendantWalletNumber) {
        return false;
    }
}
