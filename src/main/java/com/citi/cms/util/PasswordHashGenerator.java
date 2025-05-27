package com.citi.cms.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes for test users
 * Run this as a simple Java main method to generate hashes
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hash for "demo123"
        String password = "demo123";
        String hash = encoder.encode(password);
        
        System.out.println("=== BCrypt Password Hash Generator ===");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println();
        
        // Test the hash
        boolean matches = encoder.matches(password, hash);
        System.out.println("Hash verification: " + matches);
        
        System.out.println("\n=== SQL Update Statement ===");
        System.out.println("UPDATE cms_workflow.users SET password_hash = '" + hash + "' WHERE password_hash = '$2a$10$demo.hash.for.demo123';");
        
        System.out.println("\n=== Generate Multiple Hashes ===");
        String[] passwords = {"demo123", "admin123", "test123"};
        
        for (String pwd : passwords) {
            String hashValue = encoder.encode(pwd);
            System.out.println("Password: " + pwd + " => Hash: " + hashValue);
        }
    }
}