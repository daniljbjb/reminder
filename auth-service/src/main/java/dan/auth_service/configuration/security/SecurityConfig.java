/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.configuration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 *
 * @author danil
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    AuthenticationProvider exchangeAuthenticationProvider(
            PasswordEncoder encoder,
            @Value("${security.exchange.username}") String username,
            @Value("${security.exchange.password}") String password
    ) {

        UserDetailsService uds = new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(encoder.encode(password))
                        .authorities("EXCHANGE")
                        .build()
        );

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    // 1) Только для server-to-server обмена
    @Bean
    @Order(1)
    SecurityFilterChain exchangeChain(HttpSecurity http, 
            AuthenticationProvider exchangeAuthenticationProvider) throws Exception {
        http
                .securityMatcher("/api/auth/exchange")
                .authenticationProvider(exchangeAuthenticationProvider)
                .authorizeHttpRequests(registry -> registry
                    .requestMatchers(HttpMethod.POST, "/api/auth/exchange").hasAuthority("EXCHANGE")
                    .anyRequest().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/auth/exchange"))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .oauth2Login(oauth -> oauth.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }

    // 2) Всё остальное — обычный oauth2Login для людей
    @Bean
    @Order(2)
    SecurityFilterChain appChain(
            HttpSecurity http,
            AuthenticationSuccessHandler authSuccessHandler,
            AuthenticationFailureHandler authFailureHandler
    ) throws Exception {

        http
                .authorizeHttpRequests(registry -> registry
                    .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                    .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                    .successHandler(authSuccessHandler)
                    .failureHandler(authFailureHandler)
                )
                .logout(logout -> logout.logoutUrl("/logout"))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
