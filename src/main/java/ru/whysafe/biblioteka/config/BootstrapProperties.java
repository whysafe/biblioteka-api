package ru.whysafe.biblioteka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(String librarianEmail, String librarianPassword) {
}
