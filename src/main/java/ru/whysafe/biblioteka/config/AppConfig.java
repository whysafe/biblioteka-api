package ru.whysafe.biblioteka.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Библиотека API", version = "1.0",
                description = "Книги, читатели, выдачи, штрафы и очередь бронирования. "
                        + "Получите токен через POST /api/auth/login и нажмите Authorize."),
        security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class AppConfig {

    /** Все даты выдачи и штрафов считаются от этих часов, в тестах их подменяют. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
