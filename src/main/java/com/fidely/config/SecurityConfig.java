package com.fidely.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Rutas Públicas
                        .requestMatchers("/api/v1/business/register").permitAll()
                        .requestMatchers("/api/v1/business/login").permitAll()
                        .requestMatchers("/api/v1/wallet/*/download").permitAll()
                        .requestMatchers("/api/v1/wallet/onboarding").permitAll()
                        .requestMatchers("/api/v1/stripe/webhook").permitAll()
                        .requestMatchers("/api/v1/stripe/**").hasRole("BUSINESS")
                        .requestMatchers("/api/v1/scan/stream/**").permitAll()
                        .requestMatchers("/api/v1/customer/onboarding").permitAll()

                        // Swagger
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Rutas Privadas
                        // Gestión de empleados (Solo Dueño)
                        .requestMatchers("/api/v1/business/employees/**").hasRole("BUSINESS")

                        // Escaneo y Canjeo (Dueño Y Empleado)
                        .requestMatchers("/api/v1/scan/**").hasAnyRole("BUSINESS", "EMPLOYEE")

                        // Gestión de Wallet (Solo Dueño)
                        .requestMatchers("/api/v1/wallet/**").hasRole("BUSINESS")

                        // Perfil/Dashboard del Negocio (Solo Dueño)
                        .requestMatchers("/api/v1/business/**").hasRole("BUSINESS")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}