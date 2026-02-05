package com.zaphira.user.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PinService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    // Hacher le PIN lors de l'enregistrement
    public  String hashPin(String pin) {
        return encoder.encode(pin);
    }

    // Vérifier le PIN lors du déverrouillage
    public boolean verifyPin(String rawPin, String hashedPin) {
        return encoder.matches(rawPin, hashedPin);
    }
}
