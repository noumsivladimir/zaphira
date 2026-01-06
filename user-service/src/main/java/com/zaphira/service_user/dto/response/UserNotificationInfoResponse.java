package com.zaphira.service_user.dto.response;

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
    private String telegramChatId;
    private String email;
    private String firstName;
    private String lastName;
}