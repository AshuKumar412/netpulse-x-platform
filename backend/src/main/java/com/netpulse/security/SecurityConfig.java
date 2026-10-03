package com.netpulse.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint entryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationEntryPoint entryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler,
                          JwtAuthenticationFilter jwtFilter) {
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // Node Management endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/nodes/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/nodes/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/nodes/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/nodes/**").hasRole("ADMIN")

                        // Topology Management endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/topology/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/topology/links/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/topology/links/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/topology/links/**").hasRole("ADMIN")

                        // Simulation Control endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/simulation/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/simulation/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Telemetry endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/telemetry/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // Heartbeat endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/heartbeat/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/heartbeat/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Health endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/health/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")

                        // Monitoring Lifecycle endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/monitoring/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/monitoring/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Routing endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/routing/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/routing/decide").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/routing/request").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/routing/strategy").hasAnyRole("ADMIN", "OPERATOR")

                        // Failover & Self-Healing endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/failover/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/failover/recovery/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/failover/retry/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Chaos Engineering endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/chaos/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/chaos/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/chaos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/chaos/**").hasRole("ADMIN")

                        // OS Process & CPU Scheduler endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/scheduler/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/scheduler/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/scheduler/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/scheduler/**").hasAnyRole("ADMIN", "OPERATOR")

                        // What-If Infrastructure Simulator endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/what-if/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/what-if/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/what-if/**").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/what-if/**").hasAnyRole("ADMIN", "OPERATOR")

                        // Predictive Congestion Detection & ML Intelligence endpoints (RBAC)
                        .requestMatchers(HttpMethod.GET, "/api/predictions/**").hasAnyRole("ADMIN", "OPERATOR", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/predictions/run").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/predictions/verify").hasAnyRole("ADMIN", "OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/predictions/train").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/predictions/config").hasRole("ADMIN")

                        // Authenticated user endpoints
                        .requestMatchers("/api/users/me").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
