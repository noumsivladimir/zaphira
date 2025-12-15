package com.zaphira.service_user.services;

import com.zaphira.common.event.UserCreatedEvent;
import com.zaphira.common.event.WalletCreatedEvent;
import com.zaphira.service_user.dto.event.UserEventPublisher;
import com.zaphira.service_user.dto.request.*;
import com.zaphira.service_user.dto.response.*;
import com.zaphira.service_user.exception.UserAlreadyExistsException;
import com.zaphira.service_user.exception.UserNotFoundException;
import com.zaphira.service_user.kafka.UserEventProducer;
import com.zaphira.service_user.kafka.WalletResponseListener;
import com.zaphira.service_user.mapper.UserMapper;
import com.zaphira.service_user.model.entities.*;
import com.zaphira.service_user.model.enums.AccountStatus;
import com.zaphira.service_user.model.enums.AdminLevel;
import com.zaphira.service_user.repository.*;
import com.zaphira.service_user.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserEventProducer userEventProducer;
    private final RegularUserRepository regularUserRepository;
    private final UserSecurityAnswerRepository userSecurityAnswerRepository;
    private final AdminUserRepository adminUserRepository;
    private final MerchantUserRepository merchantUserRepository;
    private final WalletResponseListener walletResponseListener;
    private final PinService pinService;
    private final IpUtils ipUtils;
    private final OtpService otpService;


    //    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
   private final UserEventPublisher eventPublisher;

    private static final String UPLOAD_DIR = "uploads/profiles/";

    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new regular user with phone number: {}", request.getPhoneNumber());

        validateUserDoesNotExist(request.getPhoneNumber(), request.getEmail());

        HttpServletRequest httpServletRequest = null;

        RegularUser user = RegularUser.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
              //  .walletId(request.getWalletId())
                .pin(pinService.hashPin(request.getPin()))
                .accountLockedUntil(LocalDateTime.now().plusYears(10))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .country(request.getCountry())
                .emailVerified(false)
                .registrationDate(LocalDateTime.now())
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
             //   .lastLoginIp(ipUtils.getClientIp(httpServletRequest))
                .region(request.getRegion())
                .accountStatus(AccountStatus.ACTIVE)
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "fr")
                .build();

        RegularUser savedUser = regularUserRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());

        String correlationId = UUID.randomUUID().toString();

        CompletableFuture<WalletCreatedEvent> walletFuture =
                walletResponseListener.createPendingRequest(correlationId);


        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(user.getUserId())
                .correlationId(correlationId)
                .build();

        userEventProducer.publishUserCreatedEvent(event);



        // eventPublisher.publishUserRegisteredEvent(savedUser);

        String walletId = null;
        try {
            WalletCreatedEvent walletEvent = walletFuture.get(30, TimeUnit.SECONDS);
            if (walletEvent.isSuccess()) {
                walletId = walletEvent.getWalletId();
                // Optionnel: sauvegarder le walletId dans l'entité User
                user.setWalletId(walletId);
                userRepository.save(user);
                log.info("Wallet created successfully with ID: {}", walletId);
            } else {
                log.error("Wallet creation failed: {}", walletEvent.getErrorMessage());
                throw new RuntimeException("Wallet creation failed: " + walletEvent.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Error waiting for wallet creation", e);
            // Gérer l'erreur (rollback, retry, etc.)
            throw new RuntimeException("Failed to create wallet for user", e);
        }

        // 6. Retourner la réponse avec le walletId
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .walletId(walletId)
                .build();
    }

//        return userMapper.toResponse(savedUser);


    @Override
    public UserResponse registerAdmin(UserRegistrationRequest request) {
        log.info("Registering new admin user with email: {}", request.getEmail());

        validateUserDoesNotExist(request.getPhoneNumber(), request.getEmail());

        AdminUser admin = AdminUser.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .country(request.getCountry())
                .accountStatus(AccountStatus.ACTIVE)
                //. neighborhood(request.neighborhood())
            //    .roleTy(request.getPosition())
                .employeeId(request.getEmployeeId())
                .adminLevel(request.getAdminLevel() != null ? request.getAdminLevel() : AdminLevel.STANDARD)
                .canManageUsers(true)
                .canAccessReports(true)
                .canManageKYC(true)
                .build();

        AdminUser savedAdmin = adminUserRepository.save(admin);
        log.info("Admin registered successfully with ID: {}", savedAdmin.getUserId());

        return userMapper.toResponse(savedAdmin);
    }

    @Override
    public UserResponse registerMerchant(UserRegistrationRequest request) {
        log.info("Registering new merchant user with email: {}", request.getEmail());

        validateUserDoesNotExist( request.getPhoneNumber());



        MerchantUser merchant = MerchantUser.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
              //  .username(request.getUsername())
             //   .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .country(request.getCountry())
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .businessName(request.getBusinessName())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .businessAddress(request.getBusinessAddress())
                .build();

        MerchantUser savedMerchant = merchantUserRepository.save(merchant);
        log.info("Merchant registered successfully with ID: {}", savedMerchant.getUserId());

        return userMapper.toResponse(savedMerchant);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        User user = findUserEntityById(userId);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        User user = findUserEntityByEmail(email);
        return userMapper.toResponse(user);
    }




    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByWalletId(String walletId) {

        //getting user informations
        log.debug("Fetching user by Wallet Number: {}", walletId);
        User user = findUserEntityByWalletId(walletId);
        List <UserSecurityQuestionResponse > userSecurityQuestionResponses = findSecurityQuestionByWalletId(walletId);
        List <UserSecurityQuestionResponse > answers ;
//        answers.stream()
//                .map( userSecurityAnswers -> UserSecurityQuestionResponse.builder()
//                        .walletId(user.getWalletId())
//                        .question()
//
//        );

        return userMapper.userToResponse(user, userSecurityQuestionResponses);
    }


//    @Override
//    public UserResponse getUserByWalletId(String walletId) {
//        return null;
//    }

//    @Override
//    @Transactional(readOnly = true)
//    public UserResponse getUserByUsername(String username) {
//        log.debug("Fetching user by username: {}", username);
//        User user = userRepository.findByUsername(username)
//                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
//        return userMapper.toResponse(user);
//    }

    @Override
    @Transactional(readOnly = true)
    public User findUserEntityById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }


    @Override
    @Transactional(readOnly = true)
    public User findUserEntityByWalletId(String walletId) {
        return userRepository.findByWalletId(walletId)
                .orElseThrow(() -> new UserNotFoundException("User not found with walletId: " + walletId));
    }

    @Override
    public Long findUserIdByWalletId(String walletId) {
        return userRepository.findUserIdByWalletId(walletId);
    }

    @Transactional
    @Override
    public List<UserSecurityQuestionResponse> findSecurityQuestionByWalletId(String walletId) {

        Long userId = findUserIdByWalletId(walletId);
        List <UserSecurityAnswer > answers = userSecurityAnswerRepository.findByUserQuestionId(userId);

        return answers.stream()
                .map(
                        userSecurityAnswer -> UserSecurityQuestionResponse.builder()
                                .walletId(walletId)
                                .questionId(userSecurityAnswer.getId())
                                .question(userSecurityAnswer.getQuestion().getQuestion())
                                .createdAt(userSecurityAnswer.getCreatedAt())
                                .build())
                .toList();



//        return securityQuestionRepository.findById(questionId)
//                .orElseThrow(() -> new UserNotFoundException("Security question not found with ID: " + questionId));
    }




    @Override
    @Transactional(readOnly = true)
    public User findUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Override
    public UserResponse updateUserProfile(Long userId, UpdateProfileRequest request) {
        log.info("Updating profile for user ID: {}", userId);

        User user = findUserEntityById(userId);

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (isPhoneExists(request.getPhoneNumber())) {
                throw new UserAlreadyExistsException("Phone number already in use");
            }
            user.setPhoneNumber(request.getPhoneNumber());
            user.setPhoneVerified(false);
        }
        if (request.getNeighborhood() != null) {
            user.setNeighborhood(request.getNeighborhood());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getRegion() != null) {
            user.setRegion(request.getRegion());
        }


        if (user instanceof RegularUser) {
            RegularUser regularUser = (RegularUser) user;

            if (request.getPreferredLanguage() != null) {
                regularUser.setPreferredLanguage(request.getPreferredLanguage());
            }

            if (request.getNotificationsEnabled() != null) {
                regularUser.setNotificationsEnabled(request.getNotificationsEnabled());
            }
            if (request.getEmailNotificationsEnabled() != null) {
                regularUser.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
            }
            if (request.getSmsNotificationsEnabled() != null) {
                regularUser.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
            }
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", userId);

      //  eventPublisher.publishUserUpdatedEvent(updatedUser);

        return userMapper.toResponse(updatedUser);
    }

    @Override
    public String uploadProfilePicture(Long userId, MultipartFile file) {
        log.info("Uploading profile picture for user ID: {}", userId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        User user = findUserEntityById(userId);

        if (!(user instanceof RegularUser)) {
            throw new IllegalStateException("Only regular users can upload profile pictures");
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String filename = userId + "_" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);

            Files.copy(file.getInputStream(), filePath);

            String fileUrl = "/uploads/profiles/" + filename;

            RegularUser regularUser = (RegularUser) user;
            regularUser.setProfilePicture(fileUrl);
            userRepository.save(regularUser);

            log.info("Profile picture uploaded successfully: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to upload profile picture", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public ChangePinResponse changePin(String walletId, ChangePinRequest changePinRequest) {
        log.info("Changing PIN for wallet ID: {}", walletId);

        User user = findUserEntityByWalletId(walletId);

        // Vérifier si le compte est verrouillé
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            return ChangePinResponse.builder()
                    .success(false)
                    .message("Compte verrouillé. Réessayez plus tard.")
                    .build();
        }

        // Vérifier que les nouveaux PINs correspondent (utiliser .equals() !)
        if (!changePinRequest.getNewPin().equals(changePinRequest.getNewPinConfirmation())) {
            return ChangePinResponse.builder()
                    .success(false)
                    .message("Les nouveaux PINs ne correspondent pas")
                    .build();
        }

        // Vérifier l'ancien PIN
        if (!pinService.verifyPin(changePinRequest.getOldPin(), user.getPin())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if (user.getFailedLoginAttempts() >= 3) {
                user.setAccountLockedUntil(LocalDateTime.now().plusYears(1));
                userRepository.save(user);
                return ChangePinResponse.builder()
                        .success(false)
                        .message("Trop de tentatives. Compte verrouillé.")
                        .build();
            }

            userRepository.save(user);
            int remaining = 3 - user.getFailedLoginAttempts();
            return ChangePinResponse.builder()
                    .success(false)
                    .message("Ancien PIN incorrect. " + remaining + " tentative(s) restante(s).")
                    .build();
        }

        // Vérifier que le nouveau PIN est différent de l'ancien
        if (changePinRequest.getOldPin().equals(changePinRequest.getNewPin())) {
            return ChangePinResponse.builder()
                    .success(false)
                    .message("Le nouveau PIN doit être différent de l'ancien")
                    .build();
        }

        // Mettre à jour le PIN
        user.setPin(pinService.hashPin(changePinRequest.getNewPin()));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);

        return ChangePinResponse.builder()
                .success(true)
                .message("PIN modifié avec succès")
                .build();
    }

    @Override
    public InitiatePinResetResponse initiateReset(InitiatePinResetRequest request) {
        return null;
    }

    @Override
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        return null;
    }

    @Override
    public VerifySecurityQuestionsResponse verifySecurityQuestions(VerifySecurityQuestionsRequest request) {
        return null;
    }

    @Override
    public ResetPinResponse resetPin(ResetPinRequest request) {
        return null;
    }


    @Override
    public UserResponse updateUserStatus(Long userId, AccountStatus status) {
        log.info("Updating status for user ID: {} to {}", userId, status);

        User user = findUserEntityById(userId);
        user.setAccountStatus(status);
        User updatedUser = userRepository.save(user);

        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void deleteUser(Long userId) {
        log.info("Deleting user with ID: {}", userId);

        User user = findUserEntityById(userId);
        userRepository.delete(user);

        log.info("User deleted successfully with ID: {}", userId);
    }

    @Override
    public void softDeleteUser(Long userId) {
        log.info("Soft deleting user with Wallet ID: {}", userId);

        User user = findUserEntityById(userId);
        user.setAccountStatus(AccountStatus.CLOSED);
        userRepository.save(user);

        log.info("User soft deleted successfully with ID: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination");
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByStatus(AccountStatus status, Pageable pageable) {
        log.debug("Fetching users by status: {}", status);
        return userRepository.findByAccountStatus(status, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
        log.debug("Searching users with keyword: {}", keyword);
        return userRepository.searchUsers(keyword, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    public void activateUser(Long userId) {
        log.info("Activating user with ID: {}", userId);
        updateUserStatus(userId, AccountStatus.ACTIVE);
    }

    @Override
    public void suspendUser(Long userId, String reason) {
        log.info("Suspending user with ID: {} for reason: {}", userId, reason);
        User user = findUserEntityById(userId);
        user.setAccountStatus(AccountStatus.SUSPENDED);
        userRepository.save(user);

      //  eventPublisher.publishUserSuspendedEvent(user, reason);
    }

    @Override
    public void lockUserAccount(Long userId, int hours) {
        log.info("Locking user account with ID: {} for {} hours", userId, hours);

        User user = findUserEntityById(userId);
        user.setAccountLockedUntil(LocalDateTime.now().plusHours(hours));
        userRepository.save(user);
    }

    @Override
    public void unlockUserAccount(Long userId) {
        log.info("Unlocking user account with ID: {}", userId);

        User user = findUserEntityById(userId);
        user.setAccountLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    @Override
    public void verifyEmail(Long userId) {
        log.info("Verifying email for user ID: {}", userId);

        User user = findUserEntityById(userId);
        user.setEmailVerified(true);
        if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION && user.getPhoneVerified()) {
            user.setAccountStatus(AccountStatus.ACTIVE);
        }
        userRepository.save(user);
    }

    @Override
    public void verifyPhone(Long userId) {
        log.info("Verifying phone for user ID: {}", userId);

        User user = findUserEntityById(userId);
        user.setPhoneVerified(true);
        if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION && user.getEmailVerified()) {
            user.setAccountStatus(AccountStatus.ACTIVE);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean isPhoneExists(String phone) {
        return false;
    }

    @Override
    public boolean isUsernameExists(String username) {
        return false;
    }

    //@Override
    @Transactional(readOnly = true)
    public boolean isPhoneNumberExists(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public boolean isUsernameExists(String username) {
//        return userRepository.existsByUsername(username);
//    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getActiveUsers() {
        return userRepository.countByAccountStatus(AccountStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getSuspendedUsers() {
        return userRepository.countByAccountStatus(AccountStatus.SUSPENDED);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getNewUsersToday() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        return userRepository.countNewUsersBetween(startOfDay, endOfDay);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getNewUsersThisMonth() {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime now = LocalDateTime.now();
        return userRepository.countNewUsersBetween(startOfMonth, now);
    }

    private void validateUserDoesNotExist(String phoneNumber, String email) {
//        if (isEmailExists(walletId)) {
//            throw new UserAlreadyExistsException("User with Wallet ID " + walletId + " already exists");
//        }
        if (phoneNumber != null && isPhoneExists(phoneNumber)) {
            throw new UserAlreadyExistsException("User with phone " + phoneNumber + " already exists");
        }
        if (email != null && isEmailExists(email)) {
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        }
    }

    public void validateUserDoesNotExist( String phoneNumber) {
        if (phoneNumber != null && isPhoneExists(phoneNumber)) {
            throw new UserAlreadyExistsException("User with phone " + phoneNumber + " already exists");
        }
    };
}