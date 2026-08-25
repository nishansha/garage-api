package com.triasoft.garage.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(AppProperties properties) {
        AppProperties.MinioProperties minioProperties = properties.getStorage().getMinio();
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(
                        minioProperties.getAccessKey(),
                        minioProperties.getSecretKey()
                )
                .build();
    }
}
