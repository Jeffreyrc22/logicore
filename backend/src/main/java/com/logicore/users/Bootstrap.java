package com.logicore.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class Bootstrap {

  @Bean
  ApplicationRunner initialAdmin(
    UserAccountRepository users,
    BCryptPasswordEncoder encoder,
    @Value("${app.admin.email:}") String email,
    @Value("${app.admin.password:}") String password
  ) {
    return args -> {
      if (email.isBlank() || password.length() < 16) return;
      if (users.findByEmailIgnoreCase(email).isPresent()) return;
      var u = new UserAccount();
      u.email = email.toLowerCase();
      u.name = "Workspace Admin";
      u.role = "SUPER_ADMIN";
      u.passwordHash = encoder.encode(password);
      users.save(u);
    };
  }
}
