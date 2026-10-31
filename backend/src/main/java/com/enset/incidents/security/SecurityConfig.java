package com.enset.incidents.security;

import com.enset.incidents.exception.ErrorResponse;
import com.enset.incidents.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // active @PreAuthorize sur les controleurs
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   UserRepository userRepository,
                                                   ObjectMapper objectMapper) throws Exception {

        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, userRepository);

        http
                .csrf(csrf -> csrf.disable())                     // API stateless avec JWT : pas de CSRF
                .cors(Customizer.withDefaults())                  // utilise le bean corsConfigurationSource
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // necessaire pour la console H2 (iframe)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
                        .requestMatchers(antMatcher("/auth/**"), antMatcher("/h2-console/**"),
                                antMatcher("/error")).permitAll()
                        .requestMatchers(antMatcher("/api/admin/**")).hasRole("ADMIN")
                        .requestMatchers(antMatcher("/api/**")).authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper)))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 401 : pas authentifie (JWT absent, invalide ou expire)
    private AuthenticationEntryPoint entryPoint(ObjectMapper mapper) {
        return (request, response, authException) -> {
            Object reason = request.getAttribute(JwtAuthFilter.ERROR_ATTRIBUTE);
            String message;
            if ("expired".equals(reason)) {
                message = "Session expiree : le token JWT a expire";
            } else if ("invalid".equals(reason)) {
                message = "Token JWT invalide";
            } else if ("disabled".equals(reason)) {
                message = "Compte desactive";
            } else {
                message = "Authentification requise : token JWT manquant";
            }
            write(response, mapper, HttpServletResponse.SC_UNAUTHORIZED, message);
        };
    }

    // 403 : authentifie mais role insuffisant (erreur levee au niveau URL, ex: /api/admin/**)
    private AccessDeniedHandler accessDeniedHandler(ObjectMapper mapper) {
        return (request, response, e) ->
                write(response, mapper, HttpServletResponse.SC_FORBIDDEN, "Acces interdit : droits insuffisants");
    }

    private void write(HttpServletResponse response, ObjectMapper mapper, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ErrorResponse(status, message));
    }

    // CORS : autorise l'application React (http://localhost:5173)
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
