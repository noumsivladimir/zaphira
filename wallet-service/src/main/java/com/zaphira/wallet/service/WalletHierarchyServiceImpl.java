package com.zaphira.wallet.service;

import com.zaphira.common.dto.WalletDTO;
import com.zaphira.wallet.dto.request.CreateSubWalletRequest;
import com.zaphira.wallet.models.entities.SubWallet;
import com.zaphira.wallet.models.entities.Wallet;
import com.zaphira.wallet.repository.SubWalletRepository;
import com.zaphira.wallet.repository.WalletRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class WalletHierarchyServiceImpl implements WalletHierarchyService{

    private final SubWalletRepository subWalletRepository;
    private final WalletService walletService;

    @Override
    public SubWallet createSubWallet(CreateSubWalletRequest request) {

        List<Wallet> walletWhichCanManage = new ArrayList<>();

        for (String walletNumber : request.getWalletNumberWhichCanManage()) {
            //check que le wallet existe

            Wallet wallet = walletRepository.findByWalletNumber(walletNumber).orElseThrow(()-> new IllegalArgumentException("Wallet with number "+walletNumber+" does not exist"));
            walletWhichCanManage.add(wallet);

        }



        SubWallet subWallet = SubWallet.builder()
                .subWalletName(request.getSubWalletName())
                .type(request.getType())
                .managingWallets(walletWhichCanManage)
                .build();

        SubWallet saved = subWalletRepository.save(subWallet);

        return saved;
    }

    @Override
    public WalletDTO getWalletWithSubWallets(String walletNumber, boolean recursive) {
        return null;
    }

    @Override
    public List<WalletDTO> getSubWallets(String walletNumber) {
        return List.of();
    }

    @Override
    public List<WalletDTO> getParentChain(String walletNumber) {
        return List.of();
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
