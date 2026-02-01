package com.zaphira.common.model.enums;

public enum PermissionType {

    SEND_MONEY,
    RECEIVE_MONEY,
    REQUEST_PAYMENT,

    // Dépôts/Retraits
    DEPOSIT_CASH,
    RECEIVE_DEPOSIT,
    WITHDRAW_CASH,

    // Paiements marchands
    PAY_MERCHANT,
    ACCEPT_PAYMENT,
    ISSUE_REFUND,

    // Opérations avancées
    SCHEDULE_TRANSACTION,
    CANCEL_TRANSACTION,
    REVERSE_TRANSACTION,

}
