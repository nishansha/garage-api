package com.triasoft.garage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app", ignoreUnknownFields = true, ignoreInvalidFields = true)
@Getter @Setter
public class AppProperties {

    private Storage storage = new Storage();

    @Getter @Setter
    public static class Storage {
        private MinioProperties minio = new MinioProperties();
    }

    @Getter @Setter
    public static class MinioProperties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }
}

