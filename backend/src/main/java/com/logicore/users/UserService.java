package com.logicore.users;

import com.logicore.audit.AuditService;
import com.logicore.auth.Access;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.session.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

  private final UserAccountRepository repo;
  private final Access access;
  private final AuditService audit;
  private final BCryptPasswordEncoder encoder;
  private final FindByIndexNameSessionRepository<? extends Session> sessions;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;

  public UserService(
    UserAccountRepository r,
    Access a,
    AuditService l,
    BCryptPasswordEncoder e,
    FindByIndexNameSessionRepository<? extends Session> s,
    org.springframework.jdbc.core.JdbcTemplate j
  ) {
    repo = r;
    access = a;
    audit = l;
    encoder = e;
    sessions = s;
    jdbc = j;
  }

  public record Input(
    @NotBlank @Size(max = 160) String name,
    @Email @NotBlank @Size(max = 254) String email,
    @Pattern(regexp = "SUPER_ADMIN|MANAGER|OPERATOR") @NotBlank String role,
    boolean active,
    @Size(max = 64) String password,
    long version
  ) {}

  @Transactional
  public UserAccount save(Long id, Input in) {
    access.admin();
    jdbc.execute("SELECT pg_advisory_xact_lock(739102)");
    var u =
      id == null
        ? new UserAccount()
        : repo
            .lock(id)
            .orElseThrow(() ->
              new ResponseStatusException(HttpStatus.NOT_FOUND)
            );
    if (
      id != null && u.version != in.version()
    ) throw new ResponseStatusException(HttpStatus.CONFLICT);
    if (
      id != null &&
      u.role.equals("SUPER_ADMIN") &&
      u.active &&
      (!in.active() || !in.role().equals("SUPER_ADMIN")) &&
      repo.countByRoleAndActiveTrue("SUPER_ADMIN") <= 1
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "Keep at least one active super administrator."
    );
    String prior = u.email;
    if (id == null || (in.password() != null && !in.password().isBlank())) {
      if (
        in.password() == null ||
        in.password().length() < 12 ||
        in.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length >
          72
      ) throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Use a 12–64 character password (maximum 72 UTF-8 bytes)."
      );
      u.passwordHash = encoder.encode(in.password());
    }
    u.name = in.name();
    u.email = in.email().trim().toLowerCase();
    u.role = in.role();
    u.active = in.active();
    repo.saveAndFlush(u);
    audit.record(
      id == null ? "CREATED" : "UPDATED",
      "USER",
      u.id,
      "Role " + u.role + ", active " + u.active
    );
    if (prior != null) sessions
      .findByPrincipalName(prior)
      .keySet()
      .forEach(sessions::deleteById);
    return u;
  }
}
