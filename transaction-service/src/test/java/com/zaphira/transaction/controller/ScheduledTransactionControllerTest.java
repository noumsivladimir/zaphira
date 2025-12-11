package com.zaphira.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaphira.transaction.repository.ScheduledTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduledTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScheduledTransactionRepository scheduledTransactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        scheduledTransactionRepository.deleteAll();
    }

    @Test
    void createAndGetScheduledTransaction_shouldWork() throws Exception {
        String payload = """
                {
                  "senderWalletNumber":"W1",
                  "receiverWalletNumber":"W2",
                  "amount":100.0,
                  "currency":"XOF",
                  "type":"P2P_TRANSFER",
                  "channel":"MOBILE",
                  "description":"Scheduled test",
                  "requestedBy":"integration-test",
                  "scheduledFor":"%s"
                }
                """.formatted(LocalDateTime.now().plusMinutes(5).toString());

        String createdBody = mockMvc.perform(post("/api/transactions/scheduled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createdBody).get("id").asLong();

        mockMvc.perform(get("/api/transactions/scheduled/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)));

        mockMvc.perform(get("/api/transactions/scheduled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}


