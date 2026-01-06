package com.zaphira.service_user.services;

import com.zaphira.common.model.enums.CameroonRegion;
import com.zaphira.common.utils.IpUtils;
import com.zaphira.service_user.client.WalletServiceClient;
import com.zaphira.service_user.dto.event.UserEventPublisher;
import com.zaphira.service_user.dto.request.UserRegistrationRequest;
import com.zaphira.service_user.dto.response.UsersRegistrationResponse;
import com.zaphira.service_user.dto.response.WalletResponse;
import com.zaphira.service_user.model.entities.AdminUser;
import com.zaphira.service_user.model.entities.MerchantUser;
import com.zaphira.service_user.model.entities.RegularUser;
import com.zaphira.service_user.model.enums.AccountStatus;
import com.zaphira.service_user.model.enums.AdminLevel;
import com.zaphira.service_user.model.enums.OtpPurpose;
import com.zaphira.service_user.repository.AdminUserRepository;
import com.zaphira.service_user.repository.MerchantUserRepository;
import com.zaphira.service_user.repository.RegularUserRepository;
//import com.zaphira.service_user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import com.zaphira.service_user.event.EmailVerificationSuccessEvent;
import com.zaphira.service_user.event.UserRegistrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.concurrent.ThreadLocalRandom;


@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserRegistrationServiceImpl implements UsersRegistrationService{

    //private final UserRepository userRepository;
    private final RegularUserRepository regularUserRepository;
    private final AdminUserRepository adminUserRepository;
    private final MerchantUserRepository merchantUserRepository;

    private final PinService pinService;
    private final HttpServletRequest servletRequest;
    private final WalletServiceClient walletServiceClient;
    private final UserServiceImpl userService;
    private final UserEventPublisher eventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OtpService otpService;


    @Override
    public UsersRegistrationResponse registerRegularUser(UserRegistrationRequest request) {
        log.info("Registering new regular user with phone number: {}", request.getPhoneNumber());

        userService.validateUserDoesNotExist(request.getPhoneNumber(), request.getEmail());

        RegularUser user = RegularUser.builder()
                .phoneNumber(request.getPhoneNumber())
                .pin(pinService.hashPin(request.getPin()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .dateOfBirth(request.getDateOfBirth())
                .country(request.getCountry())
                .registrationDate(LocalDateTime.now())
                .registrationIp(IpUtils.getClientIp(servletRequest))
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .region(request.getRegion())
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "fr")
                .build();

        RegularUser savedUser = regularUserRepository.save(user);
        log.info("User registered successfully with ID: {} - pending email verification", savedUser.getUserId());

        // Générer et envoyer l'OTP de vérification par email
        String verificationCode = otpService.generateAndSendOtp(
            request.getPhoneNumber(), 
            request.getEmail(), 
            OtpPurpose.REGISTRATION
        );
        log.info("Verification OTP sent to email: {} for user: {}", request.getEmail(), savedUser.getUserId());

        // Publier l'événement de registration pour le suivi
        applicationEventPublisher.publishEvent(new UserRegistrationEvent(
            savedUser.getUserId(),
            savedUser.getEmail(),
            savedUser.getPhoneNumber(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            verificationCode
        ));

        // NE PAS publier l'événement UserRegisteredEvent tant que l'email n'est pas vérifié
        // eventPublisher.publishUserRegisteredEvent(savedUser);

        // NE PAS créer le wallet tant que l'email n'est pas vérifié
        // createWalletForUser(savedUser.getUserId());

        // 4. Construire la réponse
        UsersRegistrationResponse response = UsersRegistrationResponse.builder()
                .userId(savedUser.getUserId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .walletId(null) // Wallet will be created after email verification
                .registrationDate(savedUser.getRegistrationDate())
                .dateOfBirth(savedUser.getDateOfBirth())
                .phoneNumber(savedUser.getPhoneNumber())
                .preferredCurrency(savedUser.getPreferredCurrency())
                .accountStatus(savedUser.getAccountStatus())
                .country(savedUser.getCountry())
                .roleType(savedUser.getRoleType())
                .build();

        // NE PAS créer le wallet tant que l'email n'est pas vérifié
        // createWalletForUser(savedUser.getUserId());

        return response;
    }

    @Override
    public UsersRegistrationResponse registerAdmin(UserRegistrationRequest request) {

        log.info("Registering new Admin user with email: {} and phone number {}", request.getEmail(), request.getPhoneNumber() );

        userService.validateUserDoesNotExist(request.getPhoneNumber(), request.getEmail());

        AdminUser user = AdminUser.builder()
                .phoneNumber(request.getPhoneNumber())
                .pin(pinService.hashPin(request.getPin()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .country(request.getCountry())
                .registrationDate(LocalDateTime.now())
                .registrationIp(IpUtils.getClientIp(servletRequest))
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .region(request.getRegion())
                .employeeId(generateEmployeeId(request.getRegionCode()))
                .accountStatus(AccountStatus.ACTIVE)
                .adminLevel(request.getAdminLevel())
                .build();

        AdminUser savedUser = adminUserRepository.save(user);
        log.info("Admin registered successfully with ID: {}", savedUser.getUserId());

        // Publish user registered event for notifications
        eventPublisher.publishUserRegisteredEvent(savedUser);

        // 4. Construire la réponse immédiatement
        UsersRegistrationResponse response = UsersRegistrationResponse.builder()
                .userId(savedUser.getUserId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .walletId(null) // Wallet will be created asynchronously
                .registrationDate(savedUser.getRegistrationDate())
                .dateOfBirth(savedUser.getDateOfBirth())
                .phoneNumber(savedUser.getPhoneNumber())
                .preferredCurrency(savedUser.getPreferredCurrency())
                .accountStatus(savedUser.getAccountStatus())
                .country(savedUser.getCountry())
                .roleType(savedUser.getRoleType())
                .build();

        // 5. Créer le wallet dans une transaction séparée pour éviter les rollbacks
        createWalletForUser(savedUser.getUserId());

        return response;
    }

    @Override
    public UsersRegistrationResponse registerMerchant(UserRegistrationRequest request) {
        log.info("Registering new merchant user with email: {} and phone number: {}", request.getEmail(), request.getPhoneNumber());

        userService.validateUserDoesNotExist(request.getPhoneNumber(), request.getEmail());

        MerchantUser user = MerchantUser.builder()
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .pin(pinService.hashPin(request.getPin()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .country(request.getCountry())
                .registrationDate(LocalDateTime.now())
                .registrationIp(IpUtils.getClientIp(servletRequest))
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .region(request.getRegion())
                .businessName(request.getBusinessName())
                .businessRegistrationNumber(request.getBusinessRegistrationNumber())
                .businessAddress(request.getBusinessAddress())
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .build();

        MerchantUser savedUser = merchantUserRepository.save(user);
        log.info("Merchant registered successfully with ID: {}", savedUser.getUserId());

        // Publish user registered event for notifications
        eventPublisher.publishUserRegisteredEvent(savedUser);

        // 4. Construire la réponse immédiatement
        UsersRegistrationResponse response = UsersRegistrationResponse.builder()
                .userId(savedUser.getUserId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .walletId(null) // Wallet will be created in separate transaction
                .registrationDate(savedUser.getRegistrationDate())
                .dateOfBirth(savedUser.getDateOfBirth())
                .phoneNumber(savedUser.getPhoneNumber())
                .email(savedUser.getEmail())
                .preferredCurrency(savedUser.getPreferredCurrency())
                .accountStatus(savedUser.getAccountStatus())
                .country(savedUser.getCountry())
                .roleType(savedUser.getRoleType())
                .build();

        // 5. Créer le wallet dans une transaction séparée
        createWalletForUser(savedUser.getUserId());

        return response;
    }

    @Override
    public UsersRegistrationResponse registerSupport(UserRegistrationRequest request) {
        // Support user registration - similar to admin but with different role
        log.info("Registering new support user with email: {} and phone number: {}", request.getEmail(), request.getPhoneNumber());

        userService.validateUserDoesNotExist(request.getPhoneNumber(), request.getEmail());

        AdminUser user = AdminUser.builder()
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .pin(pinService.hashPin(request.getPin()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .country(request.getCountry())
                .registrationDate(LocalDateTime.now())
                .registrationIp(IpUtils.getClientIp(servletRequest))
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .region(request.getRegion())
                .employeeId(generateEmployeeId(request.getRegionCode()))
                .accountStatus(AccountStatus.ACTIVE)
                .adminLevel(AdminLevel.STANDARD) // Support users have standard admin level
                .canApproveTransactions(false)
                .canManageUsers(false)
                .canAccessReports(true)
                .build();

        AdminUser savedUser = adminUserRepository.save(user);
        log.info("Support user registered successfully with ID: {}", savedUser.getUserId());

        // Publish user registered event for notifications
        eventPublisher.publishUserRegisteredEvent(savedUser);

        // 4. Construire la réponse immédiatement
        UsersRegistrationResponse response = UsersRegistrationResponse.builder()
                .userId(savedUser.getUserId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .walletId(null) // Wallet will be created in separate transaction
                .registrationDate(savedUser.getRegistrationDate())
                .dateOfBirth(savedUser.getDateOfBirth())
                .phoneNumber(savedUser.getPhoneNumber())
                .email(savedUser.getEmail())
                .preferredCurrency(savedUser.getPreferredCurrency())
                .accountStatus(savedUser.getAccountStatus())
                .country(savedUser.getCountry())
                .roleType(savedUser.getRoleType())
                .build();

        // 5. Créer le wallet dans une transaction séparée
        createWalletForUser(savedUser.getUserId());

        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createWalletForUser(Long userId) {
        try {
            WalletResponse walletResponse = walletServiceClient.createWallet(userId);
            String walletId = walletResponse.getWalletNumber();

            // Update user with wallet ID in a separate transaction
            // Try AdminUser first, then RegularUser, then MerchantUser
            AdminUser adminUser = adminUserRepository.findById(userId).orElse(null);
            if (adminUser != null) {
                adminUser.setWalletId(walletId);
                adminUserRepository.save(adminUser);
                log.info("Wallet created successfully for admin user {} with wallet ID: {}", userId, walletId);
                return;
            }

            RegularUser regularUser = regularUserRepository.findById(userId).orElse(null);
            if (regularUser != null) {
                regularUser.setWalletId(walletId);
                regularUserRepository.save(regularUser);
                log.info("Wallet created successfully for regular user {} with wallet ID: {}", userId, walletId);
                return;
            }

            MerchantUser merchantUser = merchantUserRepository.findById(userId).orElse(null);
            if (merchantUser != null) {
                merchantUser.setWalletId(walletId);
                merchantUserRepository.save(merchantUser);
                log.info("Wallet created successfully for merchant user {} with wallet ID: {}", userId, walletId);
                return;
            }

            log.warn("User with ID {} not found for wallet update", userId);

        } catch (Exception e) {
            log.warn("Failed to create wallet for user {}: {}. User registration completed successfully without wallet.",
                    userId, e.getMessage());
            // Wallet creation is optional for users
        }
    }


    @Override
    @Transactional
    public UsersRegistrationResponse verifyEmailAndActivateAccount(String email, String verificationCode) {
        log.info("Verifying email and activating account for: {}", email);

        // Vérifier l'OTP par email
        boolean isValidOtp = otpService.verifyOtpByEmail(email, verificationCode, OtpPurpose.REGISTRATION);
        if (!isValidOtp) {
            throw new IllegalArgumentException("Invalid or expired verification code");
        }

        // Trouver l'utilisateur par email
        RegularUser user = regularUserRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        // Vérifier que le compte est en attente de vérification
        if (user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Account is not pending verification");
        }

        // Activer le compte
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true); // Marquer l'email comme vérifié
        RegularUser activatedUser = regularUserRepository.save(user);
        log.info("Account activated successfully for user: {}", activatedUser.getUserId());

        // Publier l'événement de vérification email réussie
        applicationEventPublisher.publishEvent(new EmailVerificationSuccessEvent(
            activatedUser.getUserId(),
            activatedUser.getEmail(),
            verificationCode
        ));

        // Publier l'événement UserRegisteredEvent maintenant que l'email est vérifié
        eventPublisher.publishUserRegisteredEvent(activatedUser);

        // Créer le wallet maintenant que l'email est vérifié
        createWalletForUser(activatedUser.getUserId());

        // Construire la réponse
        return UsersRegistrationResponse.builder()
                .userId(activatedUser.getUserId())
                .firstName(activatedUser.getFirstName())
                .lastName(activatedUser.getLastName())
                .email(activatedUser.getEmail())
                .walletId(activatedUser.getWalletId())
                .registrationDate(activatedUser.getRegistrationDate())
                .dateOfBirth(activatedUser.getDateOfBirth())
                .phoneNumber(activatedUser.getPhoneNumber())
                .preferredCurrency(activatedUser.getPreferredCurrency())
                .accountStatus(activatedUser.getAccountStatus())
                .country(activatedUser.getCountry())
                .roleType(activatedUser.getRoleType())
                .build();
    }


    public static String generateEmployeeId(CameroonRegion region) {
        int year = Year.now().getValue();
        int random = ThreadLocalRandom.current().nextInt(0, 10000);

        return String.format(
                "EMP-%d-%04d",
                year,
                random
        );
    }

}
