package com.jcore.jtransfer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;

import com.jcore.jtransfer.configuration.StorageProperties;
//import com.jcore.jtransfer.configuration.StorageProperties;
import com.jcore.jtransfer.service.StorageService;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class JTransferApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(JTransferApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(JTransferApplication.class, args);
	}

	@Bean
	public CommandLineRunner init(StorageService storageService) {
		return (args) -> { storageService.init(); };
	}

	@Bean
	public Java8TimeDialect java8TimeDialect() {
		return new Java8TimeDialect();
	}
}
