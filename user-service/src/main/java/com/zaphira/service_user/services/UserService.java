package com.zaphira.service_user.services;

import com.zaphira.common.event.TransactionCompletedEvent;
import com.zaphira.service_user.dto.request.UpdateProfileRequest;
import com.zaphira.service_user.dto.request.UserRegistrationRequest;
import com.zaphira.service_user.dto.response.UserResponse;
import com.zaphira.service_user.model.entities.User;
import com.zaphira.service_user.model.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    // User Registration
    UserResponse registerUser(UserRegistrationRequest request);
    UserResponse registerAdmin(UserRegistrationRequest request);
    UserResponse registerMerchant(UserRegistrationRequest request);

    // User Retrieval
    UserResponse getUserById(Long userId);
    UserResponse getUserByEmail(String email);
    UserResponse getUserByWalletId(String walletId);
    User findUserEntityById(Long userId);
    User findUserEntityByEmail(String email);

    // User Update
    UserResponse updateUserProfile(Long userId, UpdateProfileRequest request);
    UserResponse updateUserStatus(Long userId, AccountStatus status);
    String uploadProfilePicture(Long userId, MultipartFile file);

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

    // Account Status Checks
    boolean isEmailExists(String email);
    boolean isPhoneExists(String phone);
    boolean isUsernameExists(String username);

    // Statistics
    Long getTotalUsers();
    Long getActiveUsers();
    Long getSuspendedUsers();
    Long getNewUsersToday();
    Long getNewUsersThisMonth();

    // Transaction Balance Update (Event-driven)
    /**
     * Met à jour le solde de l'utilisateur après completion d'une transaction.
     * Appelé par le Kafka listener quand un événement TransactionCompletedEvent est reçu.
     * 
     * Garantit l'idempotence: chaque transaction ne sera débitée qu'une seule fois.
     * 
     * @param event L'événement de transaction complétée
     */
    void updateBalanceFromTransaction(TransactionCompletedEvent event);
}
