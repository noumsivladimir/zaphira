package com.zaphira.wallet.security.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation pour vérifier les permissions avant d'exécuter une méthode
 *
 * Exemple d'utilisation:
 *
 * @RequiresPermission(action = "TRANSACT")
 * public void debitWallet(String walletNumber, BigDecimal amount, Long userId) {
 *     // ...
 * }
 */
@Target(ElementType.METHOD)
public @interface RequiresPermission {
}
