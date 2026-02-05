package com.zaphira.user.service;

import com.zaphira.user.dto.request.*;
import com.zaphira.user.dto.response.*;
import com.zaphira.user.model.entities.User;
import com.zaphira.user.model.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    // User Registration
    UserResponse registerUser(UserRegistrationRequest request);
    UserResponse registerAdmin(UserRegistrationRequest request);
    UserResponse registerMerchant(UserRegistrationRequest request);



    // User Retrieval
    UserResponse getUserByEmail(String email);
    UserResponse getUserById(Long userId);
    UserResponse getUserByWalletId(String walletId);
    User findUserEntityByEmail(String email);
    User findUserEntityByWalletId(String walletId);
    Long findUserIdByWalletId(String walletId);
    User findUserEntityById(Long userId);

    List<UserSecurityQuestionResponse> findSecurityQuestionByWalletId(String walletId);
    // User Update
    UserResponse updateUserProfile(Long userId, UpdateProfileRequest request);
    UserResponse updateUserStatus(Long userId, AccountStatus status);
    String uploadProfilePicture(Long userId, MultipartFile file);
    ChangePinResponse changePin(String walletId, ChangePinRequest changePinRequest);


    //Pin edition
    InitiatePinResetResponse initiateReset(InitiatePinResetRequest request);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);
    VerifySecurityQuestionsResponse verifySecurityQuestions(VerifySecurityQuestionsRequest request);
    ResetPinResponse resetPin(ResetPinRequest request);


    // User Deletion
    void deleteUser(Long userId);
    void softDeleteUser(Long userId);

    // User Search & List
    Page<UserResponse> getAllUsers(Pageable pageable);
    Page<UserResponse> getUsersByStatus(AccountStatus status, Pageable pageable);
    Page<UserResponse> searchUsers(String keyword, Pageable pageable);

    // Account Management
    void activateUser(Long userId);
    void suspendUser(Long userId, String reason);
    void lockUserAccount(Long userId, int hours);
    void unlockUserAccount(Long userId);

    // Verification
    void verifyEmail(Long userId);
    void verifyPhone(Long userId);

    // Notification Info
    UserNotificationInfoResponse getUserNotificationInfo(Long userId);

    // Account Status Checks
    boolean isEmailExists(String email);
    boolean isPhoneExists(String phone);
    boolean isUsernameExists(String username);

    // Statistics
    long getTotalUsers();
    Long getActiveUsers();
    Long getSuspendedUsers();
    Long getNewUsersToday();
    Long getNewUsersThisMonth();
}
