package com.zaphira.user.dto.response;

import com.zaphira.user.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationInfoResponse {
    private String phoneNumber;
    private String email;
    private String firstName;
    private String lastName;
    private AccountStatus accountStatus;
}
