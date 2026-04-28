package com.darkness.system.management;

import com.darkness.system.management.config.JwtConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtConfig.class)
public class SystemManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemManagementApplication.class, args);
    }
}
