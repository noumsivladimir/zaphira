package com.zaphira.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping(value = "/fallback/auth", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})
    public ResponseEntity<Map<String, Object>> authFallback() {
        return buildFallback("AUTH_SERVICE_UNAVAILABLE", "Authentication service is temporarily unavailable");
    }

    @RequestMapping(value = "/fallback/users", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})
    public ResponseEntity<Map<String, Object>> usersFallback() {
        return buildFallback("USER_SERVICE_UNAVAILABLE", "User service is temporarily unavailable");
    }

    @RequestMapping(value = "/fallback/wallets", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})
    public ResponseEntity<Map<String, Object>> walletsFallback() {
        return buildFallback("WALLET_SERVICE_UNAVAILABLE", "Wallet service is temporarily unavailable");
    }

    private ResponseEntity<Map<String, Object>> buildFallback(String code, String message) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "error", "Service Unavailable",
                        "code", code,
                        "message", message
                ));
    }
}
