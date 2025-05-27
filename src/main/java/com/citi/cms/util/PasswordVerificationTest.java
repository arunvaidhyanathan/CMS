package com.citi.cms.util;

// Create this as a separate Java file or run in your IDE
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordVerificationTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Test data
        String plainPassword = "demo123";
        String expectedHash = "$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O";
        String placeholderHash = "$2a$10$demo.hash.for.demo123";
        
        System.out.println("=== PASSWORD VERIFICATION TEST ===");
        System.out.println("Plain password: '" + plainPassword + "'");
        System.out.println("Expected hash: " + expectedHash);
        System.out.println();
        
        // Test 1: Does our known good hash work?
        boolean test1 = encoder.matches(plainPassword, expectedHash);
        System.out.println("✓ Test 1 - 'demo123' matches expected hash: " + test1);
        
        // Test 2: Does placeholder hash work? (should be false)
        boolean test2 = encoder.matches(plainPassword, placeholderHash);
        System.out.println("✓ Test 2 - 'demo123' matches placeholder hash: " + test2 + " (should be false)");
        
        // Test 3: Generate a fresh hash
        String freshHash = encoder.encode(plainPassword);
        boolean test3 = encoder.matches(plainPassword, freshHash);
        System.out.println("✓ Test 3 - 'demo123' matches fresh hash: " + test3);
        System.out.println("Fresh hash: " + freshHash);
        
        System.out.println();
        System.out.println("=== SQL UPDATE COMMANDS ===");
        System.out.println("-- Using expected hash:");
        System.out.println("UPDATE cms_workflow.users SET password_hash = '" + expectedHash + "' WHERE username = 'intake.analyst';");
        System.out.println();
        System.out.println("-- Using fresh hash:");
        System.out.println("UPDATE cms_workflow.users SET password_hash = '" + freshHash + "' WHERE username = 'intake.analyst';");
        
        System.out.println();
        System.out.println("=== VERIFICATION QUERY ===");
        System.out.println("SELECT username, password_hash, LENGTH(password_hash) FROM cms_workflow.users WHERE username = 'intake.analyst';");
    }
}
