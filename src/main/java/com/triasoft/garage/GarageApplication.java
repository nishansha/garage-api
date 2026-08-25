package com.triasoft.garage;

import com.triasoft.garage.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableCaching
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties({AppProperties.class})
@SpringBootApplication(scanBasePackages = "com.triasoft.garage")
public class GarageApplication {
    public static void main(String[] args) {
        SpringApplication.run(GarageApplication.class, args);
    }
}
