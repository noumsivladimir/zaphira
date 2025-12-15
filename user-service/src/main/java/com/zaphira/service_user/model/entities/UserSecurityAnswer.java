package com.zaphira.service_user.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_security_answers")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSecurityAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private PredefinedSecurityQuestion question;

    @Column(nullable = false)
    private String answerHash;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}