package com.zaphira.wallet.dto.request;


import com.zaphira.wallet.model.enums.SubWalletType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateSubWalletRequest {

    private String subWalletName;
    private SubWalletType type;
    private ArrayList<String> walletNumberWhichCanManage;

}
