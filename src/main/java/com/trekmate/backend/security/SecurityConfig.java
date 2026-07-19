package com.trekmate.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        @Value("${app.frontend-url}")
        private String frontendUrl;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Swagger / API docs ──────────────────────────────────
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // ── Auth endpoints ──────────────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/auth/register/request-otp",
                                "/auth/register/verify",
                                "/auth/login",
                                "/auth/google",
                                "/auth/forgot-password",
                                "/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                        // ── Home / Locations / Attributes (public) ──────────────
                        .requestMatchers("/home/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/locations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/locations/**").hasAnyRole("GUIDE", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/tour-attributes/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/tour-attributes/**").hasAnyRole("GUIDE", "ADMIN")
                        // ── Tours (public) ──────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/tours/**").permitAll()
                        // ── Reviews (GET public, POST authenticated) ────────────
                        .requestMatchers(HttpMethod.GET, "/reviews/tour/**").permitAll()
                        .requestMatchers("/reviews/**").authenticated()
                        // ── Public rental equipment ─────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/v1/rental/equipments/**").permitAll()
                        // ── Weather & AI (GET public — users need to view forecast) ─
                        .requestMatchers(HttpMethod.GET, "/v1/weather/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/weather/*/refresh").hasAnyRole("GUIDE", "ADMIN")
                        // ── Bookings & Payments (authenticated) ────────────────
                        .requestMatchers("/v1/bookings/**").authenticated()
                        .requestMatchers("/v1/payments/**").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/v1/payments/payos/confirm/**").permitAll()
                        // ── Admin only ──────────────────────────────────────────
                        .requestMatchers("/admin/users/**").hasRole("ADMIN")
                        .requestMatchers("/admin/tours/**").hasAnyRole("GUIDE", "ADMIN")
                        .requestMatchers("/admin/tour-guides/**").hasAnyRole("GUIDE", "ADMIN")
                        .requestMatchers("/admin/equipment/**").hasAnyRole("GUIDE", "ADMIN")
                        // ── Default ─────────────────────────────────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.asList(
			frontendUrl,
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://localhost:5174"
                ));
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                // Cho phép tất cả headers (bao gồm Authorization, Content-Type, v.v.)
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(Arrays.asList("Authorization", "x-auth-token"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
