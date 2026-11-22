package com.catedra.catalogo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    private static final int MIN_HS256_SECRET_BYTES = 32;

    private final SecurityProperties properties;

    public SecurityConfig(SecurityProperties properties) {
        this.properties = properties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/metrics", "/actuator/metrics/**").hasAnyRole("SERVICE", "ADMIN")
                        .requestMatchers("/api/synchronization/**").hasAnyRole("SERVICE", "ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("USER", "SERVICE", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new RolesJwtAuthenticationConverter();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String secret = properties.getJwt().getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "app.security.jwt.secret es obligatorio. Configurar la variable APP_JWT_SECRET.");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_HS256_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.security.jwt.secret debe tener al menos " + MIN_HS256_SECRET_BYTES + " bytes para HS256.");
        }

        SecretKey key = new SecretKeySpec(secretBytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();

        String issuer = properties.getJwt().getIssuer();
        if (StringUtils.hasText(issuer)) {
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        }

        return decoder;
    }
}
