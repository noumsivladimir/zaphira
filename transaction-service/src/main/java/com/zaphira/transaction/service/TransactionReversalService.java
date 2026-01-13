package com.zaphira.transaction.service;

import com.zaphira.transaction.dto.TransactionDTO;

public interface TransactionReversalService {

    TransactionDTO reverseTransaction(String reference, String reason, String initiatedBy);

}
