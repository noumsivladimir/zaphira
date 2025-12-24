package com.zaphira.auth.service;

import com.zaphira.common.model.entities.User;
import com.zaphira.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Authentifie un utilisateur via phoneNumber + PIN.
     */
    public User authenticate(String phoneNumber, String pin) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .filter(user -> passwordEncoder.matches(pin, user.getPin()))
                .orElse(null);
    }


}
