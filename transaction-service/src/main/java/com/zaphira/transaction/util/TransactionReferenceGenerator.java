package com.zaphira.transaction.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
public class TransactionReferenceGenerator {

    private static final String PREFIX = "TXN";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Random RANDOM = new Random();

    /**
     * Génère une référence unique de transaction
     * Format: TXN-YYYYMMDDHHMMSS-XXXXXX
     */
    public String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String randomPart = String.format("%06d", RANDOM.nextInt(1000000));

        return String.format("%s-%s-%s", PREFIX, timestamp, randomPart);
    }
}