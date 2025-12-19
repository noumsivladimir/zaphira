package com.zaphira.service_user.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.service_user.exception.*;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class WalletServiceErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Wallet service error - Method: {}, Status: {}", methodKey, response.status());

        try {
            // Lire le corps de la réponse
            ErrorResponse errorResponse = null;
            if (response.body() != null) {
                try (InputStream inputStream = response.body().asInputStream()) {
                    errorResponse = objectMapper.readValue(inputStream, ErrorResponse.class);
                }
            }

            // Mapper les codes HTTP vers des exceptions métier
            switch (response.status()) {
                case 400:
                    return new InvalidWalletRequestException(
                            errorResponse != null ? errorResponse.getMessage() : "Bad request to wallet service"
                    );

                case 404:
                    return new WalletNotFoundException(
                            errorResponse != null ? errorResponse.getMessage() : "Wallet not found"
                    );

                case 409:
                    return new WalletAlreadyExistsException(
                            errorResponse != null ? errorResponse.getMessage() : "Wallet already exists"
                    );

                case 500:
                    return new WalletServiceException(
                            "Wallet service internal error: " +
                                    (errorResponse != null ? errorResponse.getMessage() : "Unknown error")
                    );

                case 503:
                    return new WalletServiceUnavailableException("Wallet service is temporarily unavailable");

                default:
                    return defaultDecoder.decode(methodKey, response);
            }

        } catch (IOException e) {
            log.error("Error parsing wallet service error response", e);
            return new WalletServiceException("Error communicating with wallet service", e);
        }
    }

    // Classe interne pour mapper la réponse d'erreur
    @lombok.Data
    private static class ErrorResponse {
        private String message;
        private String errorCode;
        private Integer status;
    }
}