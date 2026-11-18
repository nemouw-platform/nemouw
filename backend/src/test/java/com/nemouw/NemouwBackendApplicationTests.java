package com.nemouw;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.scheduling.enabled=false")
class NemouwBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
