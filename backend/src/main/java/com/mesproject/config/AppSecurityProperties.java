package com.mesproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
        boolean requireApiKey,
        String apiKey,
        List<String> writeRoles
) {
    // 역할 미지정 시 운영자 권한만 쓰기 허용
    public List<String> effectiveWriteRoles() {
        if (writeRoles == null || writeRoles.isEmpty()) {
            return List.of("ROLE_ADMIN", "ROLE_OPERATOR");
        }
        return writeRoles;
    }
}
