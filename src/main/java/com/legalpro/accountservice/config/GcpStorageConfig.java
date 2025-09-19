package com.legalpro.accountservice.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpStorageConfig {

    @Bean
    public Storage googleStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }
}
