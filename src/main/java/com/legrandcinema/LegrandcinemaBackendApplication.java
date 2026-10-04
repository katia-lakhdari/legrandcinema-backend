package com.legrandcinema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LegrandcinemaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LegrandcinemaBackendApplication.class, args);
	}

}