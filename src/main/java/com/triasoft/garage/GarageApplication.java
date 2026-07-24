package com.triasoft.garage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableScheduling
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = "com.triasoft.garage")
public class GarageApplication {
	public static void main(String[] args) {
		SpringApplication.run(GarageApplication.class, args);
	}
}
