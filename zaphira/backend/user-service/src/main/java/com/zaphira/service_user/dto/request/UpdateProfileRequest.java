package com.zaphira.service_user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @Size(max = 500)
    private String neighborhood;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String region;

    @Size(max = 20)

    @Size(max = 10)
    private String preferredLanguage;

    private Boolean notificationsEnabled;
    private Boolean emailNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
}