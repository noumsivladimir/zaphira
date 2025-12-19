package com.zaphira.service_user.client;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class WalletServiceFeignConfig {

    /**
     * Configuration du niveau de log
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // NONE, BASIC, HEADERS, FULL
    }

    /**
     * Configuration des timeouts
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5000, TimeUnit.MILLISECONDS,  // connectTimeout
                10000, TimeUnit.MILLISECONDS, // readTimeout
                true                           // followRedirects
        );
    }

    /**
     * Configuration du retry (désactivé car géré par Resilience4j)
     */
    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY; // Géré par @Retry de Resilience4j
    }

    /**
     * Décodeur d'erreurs personnalisé
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new WalletServiceErrorDecoder();
    }
}