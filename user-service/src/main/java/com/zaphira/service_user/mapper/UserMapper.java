package com.zaphira.service_user.mapper;

import com.zaphira.service_user.dto.response.UserResponse;
import com.zaphira.service_user.model.entities.AdminUser;
import com.zaphira.service_user.model.entities.MerchantUser;
import com.zaphira.service_user.model.entities.RegularUser;
import com.zaphira.service_user.model.entities.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .userId(user.getUserId())
           //     .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .country(user.getCountry())
                .accountStatus(user.getAccountStatus())
                .roleType(user.getRoleType())
              //  .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt());

        // Map role-specific fields
        if (user instanceof RegularUser) {
            RegularUser regularUser = (RegularUser) user;
            builder
                    .profilePicture(regularUser.getProfilePicture())
                    .preferredLanguage(regularUser.getPreferredLanguage())
                   // .notificationsEnabled(regularUser.getNotificationsEnabled())
                    .kycVerified(regularUser.isKycVerified());
        } else if (user instanceof AdminUser) {
            AdminUser adminUser = (AdminUser) user;
            builder
                    .employeeId(adminUser.getEmployeeId())
                    .adminLevel(adminUser.getAdminLevel());
        } else if (user instanceof MerchantUser) {
            MerchantUser merchantUser = (MerchantUser) user;
            builder
                    .businessName(merchantUser.getBusinessName())
                    .isVerifiedMerchant(merchantUser.getIsVerifiedMerchant());
        }

        return builder.build();
    }

    public List<UserResponse> toResponseList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }

        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}