package com.zaphira.auth.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "phone_number")
    }
)
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

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Role role;

    // Ajout du walletId pour stocker l'ID du wallet créé via wallet-service
    @Column(name = "wallet_id")
    private Long walletId;

    @Transient
    public String getFullName() {
        String fn = this.firstName == null ? "" : this.firstName.trim();
        String ln = this.lastName == null ? "" : this.lastName.trim();
        if (fn.isEmpty()) return ln;
        if (ln.isEmpty()) return fn;
        return fn + " " + ln;
    }

    public void setFullName(String fullName) {
        if (fullName == null) {
            this.firstName = null;
            this.lastName = null;
            return;
        }
        String[] parts = fullName.trim().split("\\s+", 2);
        this.firstName = parts.length > 0 ? parts[0] : null;
        this.lastName = parts.length > 1 ? parts[1] : "";
    }
}
