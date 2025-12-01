package com.zaphira.service_user.dto.request;

import com.zaphira.service_user.model.enums.AdminLevel;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationRequest {


    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;


    @Pattern(
            regexp = "^[0-9]{6}$",
            message = "PIN must be exactly 6 digits"
    )
    @Column(nullable = false, length = 6)
    private String pin;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Country is required")
    @Size(max = 100)
    private String country;

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String region;

    // Regular user specific
    private String preferredLanguage;

    // Admin user specific
    private String position;
    private String employeeId;
    private AdminLevel adminLevel;

    // Merchant user specific
    private String businessName;
    private String businessRegistrationNumber;
    private String businessAddress;

    public String getWalletId() {
        return "00000000";
    }


    public String getEmail() {
        return null;
    }
}