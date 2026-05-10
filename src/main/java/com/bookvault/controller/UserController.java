package com.bookvault.controller;

import com.bookvault.entity.User;
import com.bookvault.entity.Role;
import com.bookvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/sync")
    public ResponseEntity<User> syncUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            // Extract UID and Email from the Firebase Token
            String firebaseUid = jwt.getSubject();
            String email = jwt.getClaimAsString("email");

            // Check if user already exists in Firestore
            Optional<User> existingUser = userRepository.findByFirebaseUid(firebaseUid);

            if (existingUser.isPresent()) {
                return ResponseEntity.ok(existingUser.get());
            }

            // Create new user in Firestore if it's their first time logging in
            Role role = Role.USER;
            User newUser = User.builder()
                    .firebaseUid(firebaseUid)
                    .email(email)
                    .role(role)
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now().toString())
                    .activeDevicesCount(1) // Register the first device
                    .build();

            User savedUser = userRepository.save(newUser);
            return ResponseEntity.ok(savedUser);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
