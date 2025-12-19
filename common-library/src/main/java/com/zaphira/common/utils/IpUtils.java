package com.zaphira.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class IpUtils {

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // Prend la première IP (le vrai client)
            return ip.split(",")[0].trim();
        }

        // Fallback sur l'IP directe
        return request.getRemoteAddr();
    }
}