package com.zaphira.transaction.service.routing;

import com.zaphira.transaction.dto.requests.TransactionRequest;
import com.zaphira.transaction.model.enums.TransactionChannel;
import org.springframework.stereotype.Component;

@Component
public class RoutingService {

    public String determineRoute(TransactionRequest request) {
        
        TransactionChannel channel = request.getChannel();
//
//        if (type == TransactionType.BANK_TRANSFER) {
//            return "BANK_CORE";
//        }
//        if (type == TransactionType.CARD_TRANSFER || type == TransactionType.CONTACTLESS_PAYMENT) {
//            return "CARD_NETWORK";
//        }
        if (channel == TransactionChannel.API) {
            return "API_GATEWAY";
        }
        // default internal wallet routing
        return "WALLET_INTERNAL";
    }
}


