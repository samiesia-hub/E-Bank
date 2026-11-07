package com.SecBank.pro.Configurations;

import com.SecBank.pro.Security.CustomUserDetailsServices;
import com.SecBank.pro.Security.JwtAuthentificationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


    @Configuration
    @EnableMethodSecurity
    @AllArgsConstructor
    public class SecurityConfig {

        private final JwtAuthentificationFilter jwtAuthenticationFilter;
        private final CustomUserDetailsServices userDetailsService;



        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {

            DaoAuthenticationProvider provider =
                    new DaoAuthenticationProvider(userDetailsService);

            provider.setPasswordEncoder(passwordEncoder());

            return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(
                AuthenticationConfiguration configuration) throws Exception {

            return configuration.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http)
                throws Exception {

            http
                    .csrf(csrf -> csrf.disable())

                    .sessionManagement(session ->
                            session.sessionCreationPolicy(
                                    SessionCreationPolicy.STATELESS
                            )
                    )

                    .authenticationProvider(authenticationProvider())

                    .authorizeHttpRequests(auth -> auth

                            .requestMatchers("/api/auth/**").permitAll()

                            .requestMatchers("/api/admin/**").hasRole("ADMIN")

                            .requestMatchers("/api/support/**")
                            .hasAnyRole("SUPPORT", "ADMIN")

                            .requestMatchers("/api/client/**").hasRole("CLIENT")

                            .anyRequest().authenticated()
                    )

                    .exceptionHandling(exception -> exception
                            .authenticationEntryPoint((request, response, authException) ->
                                    response.sendError(
                                            HttpServletResponse.SC_UNAUTHORIZED,
                                            "Non authentifié"
                                    )
                            )
                    )

                    .addFilterBefore(
                            jwtAuthenticationFilter,
                            UsernamePasswordAuthenticationFilter.class
                    );

            return http.build();
        }
    }

