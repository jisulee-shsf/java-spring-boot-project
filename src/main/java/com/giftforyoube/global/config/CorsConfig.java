package com.giftforyoube.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
                "https://www.giftipie.me", "https://giftipie.me", "http://api.giftipie.me", "https://api.giftipie.me",
                "http://www.giftipie.me", "http://giftipie.me",
                "http://localhost:3000", "http://localhost:3001",
                "https://giftipie-vercel-qbjbn6707-torongs-projects.vercel.app",
                "https://giftipie-vercel-qbjbn6707-torongs-projects.vercel.app/"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.addAllowedHeader("*");
        config.addExposedHeader("*");
        config.setAllowedHeaders(List.of("Origin", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}