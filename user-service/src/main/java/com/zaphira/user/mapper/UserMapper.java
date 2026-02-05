package com.zaphira.user.mapper;

import com.zaphira.user.dto.response.ChangePinResponse;
import com.zaphira.user.dto.response.UserResponse;
import com.zaphira.user.dto.response.UserSecurityQuestionResponse;
import com.zaphira.user.model.entities.AdminUser;
import com.zaphira.user.model.entities.MerchantUser;
import com.zaphira.user.model.entities.RegularUser;
import com.zaphira.user.model.entities.User;
import lombok.Builder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Builder
public class UserMapper {


    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }


        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .walletId(user.getWalletId())

                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .country(user.getCountry())
                .city(user.getCity())
                .region(user.getRegion())
                .neighborhood(user.getNeighborhood())
                .registrationDate(String.valueOf(user.getRegistrationDate()))
                .preferredCurrency(user.getPreferredCurrency())
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

//        List<SecurityQuestionResponse> securityQuestion = securityAnswers.stream()
//                .map(sa -> SecurityQuestion.builder()
//                        .id(sa.getId())
//                        .questionId(sa.getQuestion().getId())
//                        .question(sa.getQuestion().getQuestion())
//                        .category(sa.getQuestion().getCategory().name())
//                        .createdAt(sa.getCreatedAt())
//                        .build())
//                .toList();

        return builder.build();
    }

    public UserResponse userToResponse(User user, List <UserSecurityQuestionResponse> securityQuestionResponses) {

        UserResponse userResponse = toResponse(user);
        userResponse.setSecurityQuestions(securityQuestionResponses);
        return userResponse;
    }



    public ChangePinResponse toChangePinResponse( boolean success, String message) {

        return ChangePinResponse.builder()
                .success(success)
                .message(message)
                .build();
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
