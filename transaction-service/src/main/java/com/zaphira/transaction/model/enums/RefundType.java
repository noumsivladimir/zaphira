package com.zaphira.transaction.model.enums;

/**
 * RefundType - Type of refund being processed
 */
public enum RefundType {
    /**
     * Full refund - entire transaction amount
     */
    FULL,

    /**
     * Partial refund - only a portion of the transaction
     */
    PARTIAL,

    /**
     * Fee-only refund - refund only the fees charged
     */
    FEE_ONLY
}
