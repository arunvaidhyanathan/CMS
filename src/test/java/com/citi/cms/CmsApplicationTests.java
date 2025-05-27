package com.citi.cms;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CmsApplicationTests {
/*
	@Test
	void contextLoads() {
		// This test will pass if the application context loads successfully
	}

	@Test
	void applicationStarts() {
		// This test ensures the main application class can be instantiated
		CmsApplication.main(new String[] {});
	}
		// OR add this method to your existing test class if you have one
	@Test
	public void testPasswordHashing() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		String password = "demo123";
		String hash = "$2a$10$N.zmdr9k7uOsaLQJeuOISOXzDUz5vbMRoATWY4EABP/CL/8AUed0O";
		
		boolean matches = encoder.matches(password, hash);
		System.out.println("Password 'demo123' matches hash: " + matches);
		
		assertTrue("Password should match the BCrypt hash", matches);
	} */
}