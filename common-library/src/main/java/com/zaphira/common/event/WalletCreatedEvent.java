// dto/event/WalletCreatedEvent.java
package com.zaphira.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletCreatedEvent {
    private String walletId;
    private Long userId;
    private String correlationId;
    private boolean success;
    private String errorMessage;
}