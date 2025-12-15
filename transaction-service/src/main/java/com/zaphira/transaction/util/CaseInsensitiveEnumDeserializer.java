package com.zaphira.transaction.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.zaphira.transaction.model.enums.AuthorizationMethod;

import java.io.IOException;

/**
 * Custom deserializer for AuthorizationMethod enum that ignores case when deserializing
 * Allows values like "otp", "OTP", "Otp" to be deserialized as AuthorizationMethod.OTP
 */
public class CaseInsensitiveEnumDeserializer extends JsonDeserializer<AuthorizationMethod> {

    @Override
    public AuthorizationMethod deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) {
            return null;
        }
        
        try {
            // Try case-insensitive match
            return AuthorizationMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // If not found, throw meaningful error
            throw new IllegalArgumentException(
                String.format("Invalid authorization method '%s'. Expected one of: %s",
                    value,
                    String.join(", ", enumValues())
                ),
                e
            );
        }
    }
    
    private String[] enumValues() {
        AuthorizationMethod[] methods = AuthorizationMethod.values();
        String[] values = new String[methods.length];
        for (int i = 0; i < methods.length; i++) {
            values[i] = methods[i].name();
        }
        return values;
    }
}
