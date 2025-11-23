package com.example.deployer.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
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
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**").permitAll()
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
