package com.logicore.auth;

import com.logicore.users.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final UserAccountRepository users;
  private final BCryptPasswordEncoder encoder;
  private final HttpSessionSecurityContextRepository repo;
  private final Access access;
  private final ConcurrentHashMap<String, java.util.ArrayDeque<Long>> attempts =
    new ConcurrentHashMap<>();

  public AuthController(
    UserAccountRepository u,
    BCryptPasswordEncoder e,
    HttpSessionSecurityContextRepository r,
    Access a
  ) {
    users = u;
    encoder = e;
    repo = r;
    access = a;
  }

  record Login(
    @Email @NotBlank String email,
    @NotBlank @Size(max = 72) String password
  ) {}

  @GetMapping("/csrf")
  public Object csrf(CsrfToken token) {
    return Map.of(
      "token",
      token.getToken(),
      "headerName",
      token.getHeaderName()
    );
  }

  @GetMapping("/me")
  public UserAccount me() {
    return access.user();
  }

  @PostMapping("/login")
  public UserAccount login(
    @Valid @RequestBody Login input,
    HttpServletRequest req,
    HttpServletResponse res
  ) {
    var times = attempts.computeIfAbsent(req.getRemoteAddr(), k ->
      new java.util.ArrayDeque<>()
    );
    synchronized (times) {
      long now = System.currentTimeMillis();
      while (!times.isEmpty() && times.peek() < now - 60000) times.remove();
      if (times.size() >= 20) throw new ResponseStatusException(
        HttpStatus.TOO_MANY_REQUESTS,
        "Try again in a minute."
      );
      times.add(now);
    }
    var u = users.findByEmailIgnoreCase(input.email().trim()).orElse(null);
    if (
      u == null ||
      !u.active ||
      !encoder.matches(input.password(), u.passwordHash)
    ) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Invalid email or password."
    );
    req.getSession();
    req.changeSessionId();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
      new UsernamePasswordAuthenticationToken(
        u.email,
        null,
        List.of(new SimpleGrantedAuthority("ROLE_" + u.role))
      )
    );
    SecurityContextHolder.setContext(context);
    repo.saveContext(context, req, res);
    users.recordLogin(u.id, java.time.Instant.now());
    return users.findById(u.id).orElseThrow();
  }
}

