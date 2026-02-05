package com.zaphira.user.controller;

import com.zaphira.common.dto.UserDTO;
import com.zaphira.user.dto.response.UserResponse;
import com.zaphira.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserLookupController {

    private final UserService userService;

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        log.info("Looking up user by email: {}", email);
        
        UserResponse userResponse = userService.getUserByEmail(email);
        if (userResponse == null) {
            log.warn("User not found with email: {}", email);
            return ResponseEntity.notFound().build();
        }

        // Build a minimal common UserDTO for cross-service use
        UserDTO dto = UserDTO.builder()
                .id(userResponse.getUserId())
                .email(userResponse.getEmail() != null ? userResponse.getEmail() : null)
                .fullName(userResponse.getFirstName() + " " + (userResponse.getLastName() != null ? userResponse.getLastName() : ""))
                .phoneNumber(userResponse.getPhoneNumber())
                .role(userResponse.getRoleType() != null ? userResponse.getRoleType().name() : null)
                .walletId(null)
                .build();

        log.info("User found with email: {}", email);
        return ResponseEntity.ok(dto);
    }
}
