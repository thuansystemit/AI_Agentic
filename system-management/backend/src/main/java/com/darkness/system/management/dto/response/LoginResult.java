package com.darkness.system.management.dto.response;

public record LoginResult(AuthResponse authResponse, String rawRefreshToken) {}
