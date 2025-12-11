package com.zaphira.transaction.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    private String secret;
    private long accessExpiration = 3600000; // default 1h
    private long refreshExpiration = 604800000; // default 7d
}
