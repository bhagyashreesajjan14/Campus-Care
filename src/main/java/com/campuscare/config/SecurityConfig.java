package com.campuscare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                // Public pages and assets
                .requestMatchers(
                    "/",
                    "/login",
                    "/signup",
                    "/register",
                    "/track",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()

                // Authenticated file attachments
                .requestMatchers("/attachments/**", "/files/**").authenticated()

                // Dashboard routing
                .requestMatchers("/dashboard").authenticated()

                // Student pages
                .requestMatchers("/student/**").hasRole("STUDENT")

                // Dept Admin pages
                .requestMatchers("/dept-admin/**").hasRole("DEPT_ADMIN")

                // Super Admin pages
                .requestMatchers("/admin/**", "/super-admin/**").hasAnyRole("SUPER_ADMIN", "DEPT_ADMIN")

                // Everything else
                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )

            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
