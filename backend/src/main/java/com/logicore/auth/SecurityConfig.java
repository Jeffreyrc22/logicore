package com.logicore.auth;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
public class SecurityConfig {

  @Bean
  BCryptPasswordEncoder encoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  HttpSessionSecurityContextRepository context() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  SecurityFilterChain security(
    HttpSecurity http,
    HttpSessionSecurityContextRepository repo
  ) throws Exception {
    return http
      .authorizeHttpRequests(a ->
        a
          .requestMatchers(
            "/api/v1/auth/csrf",
            "/api/v1/auth/login",
            "/api/v1/auth/recovery",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/capabilities",
            "/actuator/health",
            "/error"
          )
          .permitAll()
          .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
          .hasRole("SUPER_ADMIN")
          .requestMatchers(org.springframework.http.HttpMethod.GET, "/", "/login", "/recovery", "/reset-password", "/dashboard", "/operations", "/documents", "/inventory", "/customers", "/suppliers", "/users", "/audit", "/reports", "/settings", "/index.html", "/assets/**", "/images/**", "/favicon.ico", "/*.js", "/*.css", "/*.svg", "/*.woff2")
          .permitAll()
          .anyRequest()
          .authenticated()
      )
      .securityContext(s -> s.securityContextRepository(repo))
      .csrf(c ->
        c.csrfTokenRepository(
          new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository()
        )
      )
      .exceptionHandling(e ->
        e
          .authenticationEntryPoint((req, res, ex) -> {
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write("{\"message\":\"Sign in to continue.\"}");
          })
          .accessDeniedHandler((req, res, ex) -> {
            res.setStatus(403);
            res.setContentType("application/json");
            res
              .getWriter()
              .write(
                "{\"message\":\"Request not permitted. Refresh your session.\"}"
              );
          })
      )
      .logout(l ->
        l
          .logoutUrl("/api/v1/auth/logout")
          .invalidateHttpSession(true)
          .clearAuthentication(true)
          .logoutSuccessHandler((req, res, a) -> res.setStatus(204))
      )
      .build();
  }
}
