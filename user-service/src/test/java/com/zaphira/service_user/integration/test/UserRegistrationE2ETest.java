package com.zaphira.service_user.integration.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.common.model.enums.CameroonRegion;
import com.zaphira.service_user.dto.request.UserRegistrationRequest;
import com.zaphira.service_user.dto.response.UsersRegistrationResponse;
import com.zaphira.service_user.model.entities.AdminUser;
import com.zaphira.service_user.repository.AdminUserRepository;
import com.zaphira.service_user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Integration Test for User Registration
 * 
 * Tests the complete flow:
 * 1. User registration via REST API
 * 2. User saved to SQLite H2 database
 * 3. Wallet creation via Feign (synchronous)
 * 4. User updated with wallet ID
 * 
 * Uses "test-sync" profile which:
 * - Replaces PostgreSQL with H2 SQLite
 * - Replaces Kafka with Feign REST calls
 * - Allows testing on a single machine without external dependencies
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-sync")
@Slf4j
@DisplayName("User Registration Integration Test (SQLite + Feign)")
public class UserRegistrationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        userRepository.deleteAll();
        log.info("✅ Database cleaned - Ready for test");
    }

    @Test
    @DisplayName("Should successfully register Admin user with wallet creation")
    void testAdminRegistrationSuccess() throws Exception {
        // Given: Valid registration request
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .phoneNumber("+237698766472")
                .email("admin@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .pin("123456")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .country("Cameroon")
                .region("DOUALA")
                .city("Douala")
                .neighborhood("Bepanda")
                .regionCode(CameroonRegion.LT)
                .build();

        String requestBody = objectMapper.writeValueAsString(request);

        // When: POST to registration endpoint
        MvcResult result = mockMvc.perform(post("/api/users/register/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                // Then: Expect success response
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.firstName").value("Jean"))
                .andExpect(jsonPath("$.lastName").value("Dupont"))
                .andExpect(jsonPath("$.walletId").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        UsersRegistrationResponse response = objectMapper.readValue(
                responseBody, UsersRegistrationResponse.class);

        // Verify response
        log.info("✅ Registration response: {}", response);
        assertThat(response.getUserId()).isNotNull();
        assertThat(response.getWalletId()).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("Jean");
    }

    @Test
    @DisplayName("Should persist Admin user in SQLite database")
    void testAdminUserPersistence() throws Exception {
        // Given: Valid registration request
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .phoneNumber("+237123456789")
                .email("persist-test@test.com")
                .firstName("Marie")
                .lastName("Martin")
                .pin("654321")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .country("Cameroon")
                .region("YAOUNDE")
                .city("Yaounde")
                .neighborhood("Mvog-Mbi")
                .regionCode(CameroonRegion.CE)
                .build();

        // When: Register admin
        mockMvc.perform(post("/api/users/register/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then: Verify user is persisted in database
        var savedUser = userRepository.findByPhoneNumber("+237123456789");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getFirstName()).isEqualTo("Marie");
        assertThat(savedUser.get().getEmail()).isEqualTo("persist-test@test.com");
        
        log.info("✅ User persisted successfully: {}", savedUser.get());
    }

    @Test
    @DisplayName("Should reject duplicate email registration")
    void testDuplicateEmailRejection() throws Exception {
        // Given: First user registered
        UserRegistrationRequest firstRequest = UserRegistrationRequest.builder()
                .phoneNumber("+237111111111")
                .email("duplicate@test.com")
                .firstName("John")
                .lastName("Doe")
                .pin("111111")
                .dateOfBirth(LocalDate.of(1992, 3, 10))
                .country("Cameroon")
                .region("DOUALA")
                .city("Douala")
                .neighborhood("Bepanda")
                .regionCode(CameroonRegion.LT)
                .build();

        mockMvc.perform(post("/api/users/register/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

        // When: Second user with same email
        UserRegistrationRequest secondRequest = UserRegistrationRequest.builder()
                .phoneNumber("+237222222222")
                .email("duplicate@test.com")
                .firstName("Jane")
                .lastName("Smith")
                .pin("222222")
                .dateOfBirth(LocalDate.of(1993, 7, 20))
                .country("Cameroon")
                .region("YAOUNDE")
                .city("Yaounde")
                .neighborhood("Mvog-Mbi")
                .regionCode(CameroonRegion.CE)
                .build();

        // Then: Expect conflict error
        mockMvc.perform(post("/api/users/register/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict());

        log.info("✅ Duplicate email rejected as expected");
    }

    @Test
    @DisplayName("Should validate required fields")
    void testValidationOfRequiredFields() throws Exception {
        // Given: Invalid request (missing phone number)
        UserRegistrationRequest invalidRequest = UserRegistrationRequest.builder()
                .email("invalid@test.com")
                .firstName("Test")
                .lastName("User")
                .pin("123456")
                .build();

        // When & Then: Expect validation error
        mockMvc.perform(post("/api/users/register/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        log.info("✅ Validation error caught as expected");
    }
}
