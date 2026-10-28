package com.nemouw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NemouwBackendApplication {

	static void main(String[] args) {
		SpringApplication.run(NemouwBackendApplication.class, args);
	}

}
