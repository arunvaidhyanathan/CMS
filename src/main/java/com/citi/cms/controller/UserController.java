package com.citi.cms.controller;

import com.citi.cms.entity.User;
import com.citi.cms.security.UserPrincipal;
import com.citi.cms.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers(@AuthenticationPrincipal UserPrincipal currentUser) {
        logger.info("Retrieving all users by admin: {}", currentUser.getUsername());
        
        try {
            List<User> users = userService.getAllUsers();
            
            // Remove password hash from response for security
            users.forEach(user -> user.setPasswordHash(null));
            
            logger.info("Retrieved {} users", users.size());
            
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            logger.error("Error retrieving users: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.getUserById(#id).orElse(new com.citi.cms.entity.User()).getUserId() == authentication.principal.userId")
    public ResponseEntity<User> getUserById(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving user by ID: {} by user: {}", id, currentUser.getUsername());
        
        try {
            Optional<User> userOptional = userService.getUserById(id);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPasswordHash(null); // Remove password hash for security
                
                logger.info("User found: {}", user.getUsername());
                
                return ResponseEntity.ok(user);
            } else {
                logger.warn("User not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving user by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user-id/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<User> getUserByUserId(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving user by userId: {} by user: {}", userId, currentUser.getUsername());
        
        try {
            Optional<User> userOptional = userService.getUserByUserId(userId);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPasswordHash(null); // Remove password hash for security
                
                logger.info("User found by userId: {} - {}", userId, user.getUsername());
                
                return ResponseEntity.ok(user);
            } else {
                logger.warn("User not found with userId: {}", userId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving user by userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.principal.username")
    public ResponseEntity<User> getUserByUsername(
            @PathVariable String username,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Retrieving user by username: {} by user: {}", username, currentUser.getUsername());
        
        try {
            Optional<User> userOptional = userService.getUserByUsername(username);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPasswordHash(null); // Remove password hash for security
                
                logger.info("User found by username: {}", username);
                
                return ResponseEntity.ok(user);
            } else {
                logger.warn("User not found with username: {}", username);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving user by username {}: {}", username, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getUserProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        logger.info("Retrieving user profile for: {}", currentUser.getUsername());
        
        try {
            Optional<User> userOptional = userService.getUserByUserId(currentUser.getUserId());
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPasswordHash(null); // Remove password hash for security
                
                logger.info("User profile retrieved for: {}", user.getUsername());
                
                return ResponseEntity.ok(user);
            } else {
                logger.warn("User profile not found for userId: {}", currentUser.getUserId());
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving user profile for userId {}: {}", 
                        currentUser.getUserId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> createUser(
            @RequestBody User user,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Creating user: {} by admin: {}", user.getUsername(), currentUser.getUsername());
        
        try {
            // Check if username already exists
            if (userService.existsByUsername(user.getUsername())) {
                logger.warn("Username already exists: {}", user.getUsername());
                return ResponseEntity.badRequest().build();
            }
            
            // Check if email already exists
            if (userService.existsByEmail(user.getEmail())) {
                logger.warn("Email already exists: {}", user.getEmail());
                return ResponseEntity.badRequest().build();
            }
            
            User createdUser = userService.createUser(user);
            createdUser.setPasswordHash(null); // Remove password hash for security
            
            logger.info("User created successfully: {}", createdUser.getUsername());
            
            return ResponseEntity.ok(createdUser);
            
        } catch (Exception e) {
            logger.error("Error creating user {}: {}", user.getUsername(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.getUserById(#id).orElse(new com.citi.cms.entity.User()).getUserId() == authentication.principal.userId")
    public ResponseEntity<User> updateUser(
            @PathVariable Integer id,
            @RequestBody User user,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Updating user ID: {} by user: {}", id, currentUser.getUsername());
        
        try {
            Optional<User> existingUserOptional = userService.getUserById(id);
            
            if (!existingUserOptional.isPresent()) {
                logger.warn("User not found for update with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            user.setId(id); // Ensure the ID is set correctly
            User updatedUser = userService.updateUser(user);
            updatedUser.setPasswordHash(null); // Remove password hash for security
            
            logger.info("User updated successfully: {}", updatedUser.getUsername());
            
            return ResponseEntity.ok(updatedUser);
            
        } catch (Exception e) {
            logger.error("Error updating user ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        logger.info("Deleting user ID: {} by admin: {}", id, currentUser.getUsername());
        
        try {
            Optional<User> userOptional = userService.getUserByUserId(id);
            
            if (!userOptional.isPresent()) {
                logger.warn("User not found for deletion with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            userService.deleteUser(id);
            
            logger.info("User deleted successfully with ID: {}", id);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error deleting user ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        logger.info("User service health check");
        return ResponseEntity.ok("User service is running");
    }

    @GetMapping("/version")
    public ResponseEntity<String> getVersion() {
        logger.info("User service version check");
        return ResponseEntity.ok("User Service Version 1.0.0");
    }
}