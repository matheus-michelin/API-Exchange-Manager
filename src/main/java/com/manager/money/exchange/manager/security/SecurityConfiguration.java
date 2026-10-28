package com.manager.money.exchange.manager.security;

import com.manager.money.exchange.manager.RateLimiter.RateLimitFilter;
import com.manager.money.exchange.manager.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final RateLimitFilter rateLimitFilter;
    private final SecurityFilter securityFilter;

    private final List<String> allowedOrigins;

    public SecurityConfiguration(RateLimitFilter rateLimitFilter,
                                 SecurityFilter securityFilter,
                                 @Value("${app.cors.allowed-origins=http://localhost:3000,http://127.0.0.1:3000,http://127.0.0.1:5050,http://127.0.0.1:5051,http://127.0.0.1:5501,http://localhost:5501}")
                                 List<String> allowedOrigins) {
        this.rateLimitFilter = rateLimitFilter;
        this.securityFilter = securityFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        return http.csrf(c -> c.disable())
                // CORS roda ANTES da autorização: o preflight (OPTIONS) não leva Authorization
                // e seria barrado por anyRequest().authenticated() sem isso.
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setCharacterEncoding("UTF-8");
                    res.setContentType("application/json");
                    res.getWriter().write("{\"campo\":null,\"mensagem\":\"Não autenticado\"}");
                }))
                .authorizeHttpRequests(r -> r
                        .requestMatchers(HttpMethod.POST, "/login", "/login/cadastro").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, SecurityFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Sem isso o navegador esconde o Retry-After do 429 e o front usa um tempo padrão.
        config.setExposedHeaders(List.of("Retry-After"));
        // O token vai no header Authorization, não em cookie.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }


    @Bean
    FilterRegistrationBean<SecurityFilter> securityFilterRegistration(SecurityFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository repository) {
        return login -> repository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException(login));
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
