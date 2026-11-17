package com.cronos.payment.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiAccessRules rules,
                                                   @Value("${app.security.internal-token}") String internalToken) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/actuator/health/**").permitAll();
                    auth.requestMatchers("/internal/**").hasRole("SERVICE");
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    rules.configure(auth);
                    auth.anyRequest().denyAll();
                })
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> write(res, HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Authentication required"))
                        .accessDeniedHandler((req, res, ex) -> write(res, HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied")))
                .addFilterBefore(new InternalAuthFilter(internalToken), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** No local accounts exist in this service; this only stops Spring Boot from generating a default user. */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    private static void write(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\",\"code\":\"" + code + "\"}");
    }
}
