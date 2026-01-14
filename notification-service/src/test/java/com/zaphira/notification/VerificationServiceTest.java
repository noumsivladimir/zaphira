package com.zaphira.notification;

import com.zaphira.notification.model.VerificationToken;
import com.zaphira.notification.repository.VerificationTokenRepository;
import com.zaphira.notification.service.VerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private VerificationTokenRepository tokenRepository;

    @InjectMocks
    private VerificationService verificationService;

    @Test
    void generateAndSaveCode_shouldGenerateAndSaveToken() {
        // Given
        Long userId = 1L;
        when(tokenRepository.save(any(VerificationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        String code = verificationService.generateAndSaveCode(userId);

        // Then
        assertNotNull(code);
        assertEquals(6, code.length());
        verify(tokenRepository).save(any(VerificationToken.class));
    }

    @Test
    void verifyCode_shouldReturnTrueForValidCode() {
        // Given
        Long userId = 1L;
        String code = "123456";
        VerificationToken token = VerificationToken.builder()
                .userId(userId)
                .code(code)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(tokenRepository.findByUserIdAndCodeAndUsedFalseAndExpiresAtAfter(eq(userId), eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(token));

        // When
        boolean result = verificationService.verifyCode(userId, code);

        // Then
        assertTrue(result);
        verify(tokenRepository).markAsUsed(userId, code);
    }

    @Test
    void verifyCode_shouldReturnFalseForInvalidCode() {
        // Given
        Long userId = 1L;
        String code = "123456";
        when(tokenRepository.findByUserIdAndCodeAndUsedFalseAndExpiresAtAfter(eq(userId), eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // When
        boolean result = verificationService.verifyCode(userId, code);

        // Then
        assertFalse(result);
        verify(tokenRepository, never()).markAsUsed(any(), any());
    }
}