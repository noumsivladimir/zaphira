package com.zaphira.common.dto.request;


import com.zaphira.common.model.enums.SubWalletType;

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
