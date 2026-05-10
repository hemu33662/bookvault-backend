package com.bookvault.controller;

import com.bookvault.entity.BookAccess;
import com.bookvault.entity.User;
import com.bookvault.repository.BookAccessRepository;
import com.bookvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/access")
public class BookAccessController {

    @Autowired
    private com.bookvault.service.SecurityService securityService;

    @Autowired
    private com.bookvault.repository.BookAccessRepository bookAccessRepository;
    
    @Autowired
    private com.bookvault.repository.UserRepository userRepository;

    @GetMapping("/my-library")
    public ResponseEntity<List<BookAccess>> getMyLibrary() {
        String firebaseUid = securityService.getCurrentUserFirebaseUid();
        if (firebaseUid == null) return ResponseEntity.status(401).build();
        
        try {
            // First try finding by Firebase UID
            List<BookAccess> access = bookAccessRepository.findByUserId(firebaseUid);
            if (access.isEmpty()) {
                // Try internal ID
                Optional<User> user = userRepository.findByFirebaseUid(firebaseUid);
                if (user.isPresent()) {
                    access = bookAccessRepository.findByUserId(user.get().getId());
                }
            }
            return ResponseEntity.ok(access);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkAccess(@RequestParam String bookId) {
        String firebaseUid = securityService.getCurrentUserFirebaseUid();
        if (firebaseUid == null) return ResponseEntity.ok(false);
        
        try {
            // 1. Check with Firebase UID
            boolean hasAccess = bookAccessRepository.findByUserIdAndBookId(firebaseUid, bookId).isPresent();
            if (hasAccess) return ResponseEntity.ok(true);
            
            // 2. Try Internal ID
            Optional<User> user = userRepository.findByFirebaseUid(firebaseUid);
            if (user.isPresent()) {
                hasAccess = bookAccessRepository.findByUserIdAndBookId(user.get().getId(), bookId).isPresent();
                if (hasAccess) return ResponseEntity.ok(true);
            }

            return ResponseEntity.ok(false);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
