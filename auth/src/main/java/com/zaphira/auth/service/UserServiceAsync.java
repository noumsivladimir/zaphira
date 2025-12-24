package com.zaphira.auth.service;

import com.zaphira.auth.repository.UserRepository;
import com.zaphira.common.model.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service("userServiceAsync")
@RequiredArgsConstructor
public class UserServiceAsync {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Authentifie un utilisateur via phoneNumber + PIN.
     */
    public User authenticate(String phoneNumber, String rawPin) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .filter(user -> passwordEncoder.matches(rawPin, user.getPin()))
                .orElse(null);
    }

    /**
     * Récupère un utilisateur via phoneNumber.
     */
    public User findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).orElse(null);
    }


}
