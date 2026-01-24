package com.zaphira.service_user.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PinService {

    private final PasswordEncoder encoder;

    public PinService(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    // Hacher le PIN lors de l'enregistrement
    public  String hashPin(String pin) {
        return encoder.encode(pin);
    }

    // Vérifier le PIN lors du déverrouillage
    public boolean verifyPin(String rawPin, String hashedPin) {
        return encoder.matches(rawPin, hashedPin);
    }
}