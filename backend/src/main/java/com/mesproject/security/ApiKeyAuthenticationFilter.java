package com.mesproject.security;

import com.mesproject.config.AppSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_API_KEY = "X-API-KEY";
    private static final String HEADER_USER_ID = "X-USER-ID";
    private static final String HEADER_USER_ROLE = "X-USER-ROLE";

    private final AppSecurityProperties securityProperties;

    public ApiKeyAuthenticationFilter(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        boolean writeRequest = HttpMethod.POST.matches(request.getMethod())
                || HttpMethod.PATCH.matches(request.getMethod())
                || HttpMethod.PUT.matches(request.getMethod())
                || HttpMethod.DELETE.matches(request.getMethod());

        // 개발/데모 환경에서는 API Key 강제를 끌 수 있도록 지원
        if (!securityProperties.requireApiKey()) {
            filterChain.doFilter(request, response);
            return;
        }

        String expectedApiKey = securityProperties.apiKey();
        String providedApiKey = request.getHeader(HEADER_API_KEY);

        // 쓰기 요청은 반드시 API Key 필요
        if (writeRequest && (!StringUtils.hasText(expectedApiKey) || !expectedApiKey.equals(providedApiKey))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid API key\"}");
            return;
        }

        // 유효 키가 있으면 사용자 컨텍스트 등록(감사로그/권한판단용)
        if (StringUtils.hasText(expectedApiKey) && expectedApiKey.equals(providedApiKey)) {
            String userId = StringUtils.hasText(request.getHeader(HEADER_USER_ID))
                    ? request.getHeader(HEADER_USER_ID)
                    : "api-user";

            String role = StringUtils.hasText(request.getHeader(HEADER_USER_ROLE))
                    ? request.getHeader(HEADER_USER_ROLE)
                    : "ROLE_OPERATOR";

            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role.toUpperCase();
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    "N/A",
                    List.of(new SimpleGrantedAuthority(role))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
