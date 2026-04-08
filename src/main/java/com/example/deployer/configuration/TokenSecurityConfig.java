package com.example.deployer.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import com.example.deployer.tokens.TokenAuthenticationProvider;
import com.example.deployer.tokens.TokenCleanupLogoutHandler;

@Configuration
@EnableWebSecurity
public class TokenSecurityConfig {

    private final TokenAuthenticationProvider tokenAuthenticationProvider;
    private final TokenCleanupLogoutHandler tokenCleanupLogoutHandler;

    public TokenSecurityConfig(TokenAuthenticationProvider tokenAuthenticationProvider, TokenCleanupLogoutHandler tokenCleanupLogoutHandler) {
        this.tokenAuthenticationProvider = tokenAuthenticationProvider;
        this.tokenCleanupLogoutHandler = tokenCleanupLogoutHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // #4 Defensive security headers
            .headers(headers -> headers
                // Prevent clickjacking
                .frameOptions(frame -> frame.deny())
                // Prevent MIME-sniffing
                .contentTypeOptions(Customizer.withDefaults())
                // Enforce HTTPS on future requests
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )
            // CSRF cookie must stay readable by JS (Alpine.js reads XSRF-TOKEN to set the header)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            )
            .authorizeHttpRequests(auth -> auth
                // Public: login UI and static assets
                .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                // Actuator endpoints and API docs are public
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .addLogoutHandler(tokenCleanupLogoutHandler)
                .permitAll()
            )
            .authenticationProvider(tokenAuthenticationProvider);

        return http.build();
    }

}
