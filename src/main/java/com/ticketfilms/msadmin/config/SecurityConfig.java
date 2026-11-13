package com.ticketfilms.msadmin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import com.ticketfilms.msadmin.security.CognitoGruposConverter;

// Toda la API de administración exige un JWT válido de Cognito Y pertenecer
// al grupo admin. Lo demás se deniega (excepto el health check).
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            @Value("${admin.grupo:admin}") String grupoAdmin) throws Exception {

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(new CognitoGruposConverter());

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(grupoAdmin)
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor)))
                // --- AGREGADO DESDE NOTIFICACIONES: Logs de seguridad ---
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            log.warn("401 no autenticado — path={} motivo={}", request.getRequestURI(), e.getMessage());
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED).commence(request, response, e);
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            log.warn("403 acceso denegado — path={} motivo={}", request.getRequestURI(), e.getMessage());
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                        }));

        return http.build();
    }
}
