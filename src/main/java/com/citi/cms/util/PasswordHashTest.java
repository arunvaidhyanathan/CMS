package com.citi.cms.util;

// ADD THIS SIMPLE TEST METHOD TO YOUR AuthServiceImpl OR CREATE A TEST CLASS

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashTest {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String plainPassword = "demo123";
        String knownWorkingHash = "$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O";
        
        System.out.println("=== PASSWORD HASH TEST ===");
        System.out.println("Plain password: " + plainPassword);
        System.out.println("Known hash: " + knownWorkingHash);
        
        boolean matches = encoder.matches(plainPassword, knownWorkingHash);
        System.out.println("Password matches hash: " + matches);
        
        // Generate a new hash for comparison
        String newHash = encoder.encode(plainPassword);
        System.out.println("Newly generated hash: " + newHash);
        
        boolean newMatches = encoder.matches(plainPassword, newHash);
        System.out.println("Password matches new hash: " + newMatches);
        
        System.out.println("=== COPY THIS SQL ===");
        System.out.println("UPDATE cms_workflow.users SET password_hash = '" + newHash + "' WHERE username = 'intake.analyst';");
    }
}
