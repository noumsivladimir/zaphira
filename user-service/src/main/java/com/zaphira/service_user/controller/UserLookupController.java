//package com.zaphira.service_user.controller;
//
//import com.zaphira.common.dto.UserDTO;
//import com.zaphira.service_user.services.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/users")
//@RequiredArgsConstructor
//public class UserLookupController {
//
//    private final UserService userService;
//
//    @GetMapping("/email/{email}")
//    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
//        var userResponse = userService.getUserByEmail(email);
//        if (userResponse == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        // Build a minimal common UserDTO for cross-service use
//        UserDTO dto = UserDTO.builder()
//                .id(userResponse.getUserId())
//                .email(userResponse.getEmaxil() != null ? userResponse.getEmaxil() : null)
//                .fullName(userResponse.getFirstName() + " " + (userResponse.getLastName() != null ? userResponse.getLastName() : ""))
//                .phoneNumber(userResponse.getPhoneNumber())
//                .role(userResponse.getRoleType() != null ? userResponse.getRoleType().name() : null)
//                .walletId(null)
//                .build();
//
//        return ResponseEntity.ok(dto);
//    }
//}
