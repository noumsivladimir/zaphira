package com.zaphira.zaphira;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class ActivityLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;

    private LocalDateTime timestamp = LocalDateTime.now();

    @ManyToOne
    private User user;
}
