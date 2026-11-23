package com.nemouw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Arrays;
import java.util.Map;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NemouwBackendApplication {

	static void main(String[] args) {
		SpringApplication app = new SpringApplication(NemouwBackendApplication.class);
		if (Arrays.asList(args).contains("--create-user")) {
			// Lightweight boot: no web server, no JPA, beans created only when used
			app.setDefaultProperties(Map.of(
					"spring.main.web-application-type", "none",
					"spring.main.lazy-initialization", "true",
					"spring.autoconfigure.exclude", String.join(",",
							"org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
							"org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration")));
		}
		app.run(args);
	}

}
