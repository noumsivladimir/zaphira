package com.zaphira.service_user.services;

import com.zaphira.common.model.enums.CameroonRegion;
import com.zaphira.common.utils.IpUtils;
import com.zaphira.service_user.client.WalletServiceClient;
import com.zaphira.service_user.dto.request.CreateWalletRequest;
import com.zaphira.service_user.dto.request.UserRegistrationRequest;
import com.zaphira.service_user.dto.response.UsersRegistrationResponse;
import com.zaphira.service_user.dto.response.WalletResponse;
import com.zaphira.service_user.model.entities.AdminUser;
import com.zaphira.service_user.model.entities.RegularUser;
import com.zaphira.service_user.model.enums.AccountStatus;
import com.zaphira.service_user.repository.AdminUserRepository;
import com.zaphira.service_user.repository.MerchantUserRepository;
import com.zaphira.service_user.repository.RegularUserRepository;
import com.zaphira.service_user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.concurrent.ThreadLocalRandom;


@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserRegistrationServiceImpl implements UsersRegistrationService{

    private final UserRepository userRepository;
    private final RegularUserRepository regularUserRepository;
    private final AdminUserRepository adminUserRepository;
    private final MerchantUserRepository merchantUserRepository;

    private final PinService pinService;
    private final HttpServletRequest servletRequest;
    private final WalletServiceClient walletServiceClient;
    private final UserServiceImpl userService;


    @Override
    public UsersRegistrationResponse registerRegularUser(UserRegistrationRequest request) {
        log.info("Registering new regular user with phone number: {}", request.getPhoneNumber());

        userService.validateUserDoesNotExist(request.getPhoneNumber());

        RegularUser user = RegularUser.builder()
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
                .accountStatus(AccountStatus.ACTIVE)
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "fr")
                .build();

        RegularUser savedUser = regularUserRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getUserId());

        String walletId = null;

        try {
            CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                    .userId(savedUser.getUserId())
                    .build();

            WalletResponse walletResponse = walletServiceClient.createWallet(walletRequest);
            walletId = walletResponse.getWalletNumber();
            savedUser.setWalletId(walletId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create wallet for user", e);
        }

        // 6. Retourner la réponse avec le walletId
        return UsersRegistrationResponse.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .walletId(walletId)
                .registrationDate(user.getRegistrationDate())
                .dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber())
                .preferredCurrency(user.getPreferredCurrency())
                .accountStatus(user.getAccountStatus())
                .country(user.getCountry())
                .roleType(user.getRoleType())
                .build();
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

        String walletId = null;

        try {
            CreateWalletRequest walletRequest = CreateWalletRequest.builder()
                    .userId(savedUser.getUserId())
                    .build();

            WalletResponse walletResponse = walletServiceClient.createWallet(walletRequest);
            walletId = walletResponse.getWalletNumber();
            savedUser.setWalletId(walletId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create wallet for user", e);
        }

        // 6. Retourner la réponse avec le walletId
        return UsersRegistrationResponse.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .walletId(walletId)
                .registrationDate(user.getRegistrationDate())
                .dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber())
                .preferredCurrency(user.getPreferredCurrency())
                .accountStatus(user.getAccountStatus())
                .country(user.getCountry())
                .roleType(user.getRoleType())
                .build();
    }

    @Override
    public UsersRegistrationResponse registerMerchant(UserRegistrationRequest request) {
        return null;
    }

    @Override
    public UsersRegistrationResponse registerSupport(UserRegistrationRequest request) {
        return null;
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
