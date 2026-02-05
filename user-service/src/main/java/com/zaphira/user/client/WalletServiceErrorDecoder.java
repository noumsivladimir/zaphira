package com.zaphira.user.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.user.exception.*;
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
            String errorMessage = null;
            ErrorResponse errorResponse = null;
            if (response.body() != null) {
                try (InputStream inputStream = response.body().asInputStream()) {
                    String responseBody = new String(inputStream.readAllBytes());
                    // Try to parse as JSON first
                    try {
                        errorResponse = objectMapper.readValue(responseBody, ErrorResponse.class);
                        errorMessage = errorResponse.getMessage();
                    } catch (Exception jsonException) {
                        // If JSON parsing fails, treat as plain text
                        errorMessage = responseBody;
                        log.debug("Response body is not JSON, treating as plain text: {}", errorMessage);
                    }
                }
            }

            // Mapper les codes HTTP vers des exceptions métier
            switch (response.status()) {
                case 400:
                    return new InvalidWalletRequestException(
                            errorMessage != null ? errorMessage : "Bad request to wallet service"
                    );

                case 404:
                    return new WalletNotFoundException(
                            errorMessage != null ? errorMessage : "Wallet not found"
                    );

                case 409:
                    return new WalletAlreadyExistsException(
                            errorMessage != null ? errorMessage : "Wallet already exists"
                    );

                case 500:
                    return new WalletServiceException(
                            "Wallet service internal error: " +
                                    (errorMessage != null ? errorMessage : "Unknown error")
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ErrorResponse {
        private String message;
        private String errorCode;
        private Integer status;
        private String timestamp;
    }
}
