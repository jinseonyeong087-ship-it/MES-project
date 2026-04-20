package com.mesproject.config;

import com.mesproject.security.ApiKeyAuthenticationFilter;
import com.mesproject.security.RequestAuditFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AppSecurityProperties securityProperties,
                                           ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
                                           RequestAuditFilter requestAuditFilter) throws Exception {

        String[] writeRoles = securityProperties.effectiveWriteRoles().toArray(String[]::new);

        http
                // API 서버이므로 세션을 쓰지 않는 무상태로 고정
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .xssProtection(Customizer.withDefaults())
                        .referrerPolicy(ref -> ref.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                )
                .authorizeHttpRequests(auth -> {
                    if (securityProperties.requireApiKey()) {
                        // 쓰기 API는 인증 + 역할 검증
                        auth.requestMatchers(HttpMethod.POST, "/api/**").hasAnyAuthority(writeRoles);
                        auth.requestMatchers(HttpMethod.PUT, "/api/**").hasAnyAuthority(writeRoles);
                        auth.requestMatchers(HttpMethod.PATCH, "/api/**").hasAnyAuthority(writeRoles);
                        auth.requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyAuthority(writeRoles);
                        // 조회 API는 허용(대시보드/모니터링 목적)
                        auth.anyRequest().permitAll();
                    } else {
                        // 데모/개발 환경은 전부 허용
                        auth.anyRequest().permitAll();
                    }
                })
                .addFilterBefore(requestAuditFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
