package com.darkness.system.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtConfig(
        String secret,
        int accessTokenTtlMinutes,
        int refreshTokenTtlDays
) {}
