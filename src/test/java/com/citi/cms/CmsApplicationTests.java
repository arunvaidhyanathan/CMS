package com.citi.cms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CmsApplicationTests {

	@Test
	void contextLoads() {
		// This test will pass if the application context loads successfully
	}

	@Test
	void applicationStarts() {
		// This test ensures the main application class can be instantiated
		CmsApplication.main(new String[] {});
	}
}