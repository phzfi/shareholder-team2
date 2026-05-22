package com.example.shareholder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ShareholderApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void should_StartApplication_When_MainIsCalled() {
		// Domain: the application entry-point must initialise the Spring context without errors
		ShareholderApplication.main(new String[]{"--server.port=0"});
	}

}
