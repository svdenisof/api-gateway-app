package ru.denisov.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final List<String> ALL_HEADERS = List.of("*");
    private static final String ALL_OPTIONS = "/**";
    private static final String CLAIM_AUTHORIZED_PARTY = "azp";

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ApiGatewayProperties gatewayProperties) {
        List<String> allowedOrigins = removeBlankEntries(gatewayProperties.getAllowedOrigins());
        List<String> permittedPaths = removeBlankEntries(gatewayProperties.getPermittedPaths());
        log.trace("Allowed origins {}", allowedOrigins);
        log.trace("Permitted paths {}", permittedPaths);

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource(allowedOrigins)))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(permittedPaths.toArray(new String[0])).permitAll()
                        .pathMatchers(HttpMethod.OPTIONS, ALL_OPTIONS).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            ApiGatewayProperties gatewayProperties) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> combinedValidator =
                new DelegatingOAuth2TokenValidator<>(issuerValidator, jwtValidator(gatewayProperties));
        decoder.setJwtValidator(combinedValidator);
        return decoder;
    }

    @Bean
    public OAuth2TokenValidator<Jwt> jwtValidator(ApiGatewayProperties gatewayProperties) {
        String expectedAudience = gatewayProperties.getExpectedAudience();
        return jwt -> {
            List<String> audience = jwt.getAudience();
            boolean audienceValid = expectedAudience.equals(jwt.getClaim(CLAIM_AUTHORIZED_PARTY)) ||
                    Objects.nonNull(audience) && audience.contains(expectedAudience);
            log.trace("Jwt expires at: {}", jwt.getExpiresAt());
            return audienceValid ?
                    OAuth2TokenValidatorResult.success() :
                    OAuth2TokenValidatorResult.failure(List.of(
                            new OAuth2Error("invalid_token", "Invalid client Id", null)));
        };
    }

    private CorsConfigurationSource corsConfigurationSource(List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(getMethods(HttpMethod.values()));
        configuration.setAllowedHeaders(ALL_HEADERS);
        configuration.setExposedHeaders(ALL_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ALL_OPTIONS, configuration);
        source.registerCorsConfiguration("/actuator/**", configuration);
        return source;
    }

    private static List<String> removeBlankEntries(List<String> values) {
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private static List<String> getMethods(HttpMethod... values) {
        return Stream.of(values)
                .map(HttpMethod::toString)
                .toList();
    }
}
