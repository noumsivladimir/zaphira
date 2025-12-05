package com.zaphira.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.zaphira.common.model.entities.User;

@Entity
@Data
public class ActivityLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;

    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    private User user;
}
