package com.giftforyoube.global.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("기프티파이")
                .description("펀딩 상품을 등록하고, 펀딩에 대한 후원 기능을 제공합니다.")
                .version("2.0.2")
        );
    }
}