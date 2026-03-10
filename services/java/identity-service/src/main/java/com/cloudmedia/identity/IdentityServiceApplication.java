package com.cloudmedia.identity;

import com.cloudmedia.identity.auth.config.AuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties.class)
public class IdentityServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(IdentityServiceApplication.class, args);
	}
}

@RestController
@RequestMapping("/health")
class IdentityHealthController {
	@GetMapping
	public String health() {
		return "ok";
	}
}
