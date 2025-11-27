package com.zaphira.zaphira;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users") // avoid "user" which is SQL reserved keyword
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Wallet wallet;
}
