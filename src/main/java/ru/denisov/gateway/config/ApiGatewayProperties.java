package ru.denisov.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gateway")
public class ApiGatewayProperties {

    private final List<String> permittedPaths = new ArrayList<>();
    private final List<String> allowedOrigins = new ArrayList<>();
    private String serverUri;
    private String expectedAudience = "backendAud";
}
