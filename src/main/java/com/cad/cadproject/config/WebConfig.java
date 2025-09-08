package com.cad.cadproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")              // 모든 URL에 대해
                .allowedOrigins("http://localhost:3000") // React 개발 서버 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용 HTTP 메서드
                .allowCredentials(true)          // 쿠키 전송 허용
                .maxAge(3600);                   // preflight 캐시 시간 (초)
    }
}
