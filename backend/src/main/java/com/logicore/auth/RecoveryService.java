package com.logicore.auth;

import com.logicore.users.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.session.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecoveryService {

  private final JdbcTemplate jdbc;
  private final UserAccountRepository users;
  private final BCryptPasswordEncoder encoder;
  private final FindByIndexNameSessionRepository<? extends Session> sessions;

  @Value("${app.mail.host:}")
  String host;

  @Value("${app.mail.port:587}")
  int port;

  @Value("${app.mail.username:}")
  String username;

  @Value("${app.mail.password:}")
  String password;

  @Value("${app.mail.from:}")
  String from;

  @Value("${app.public-url:http://127.0.0.1:4200}")
  String publicUrl;

  private final java.util.concurrent.ConcurrentHashMap<
    String,
    Long
  > lastRequest = new java.util.concurrent.ConcurrentHashMap<>();

  public RecoveryService(
    JdbcTemplate j,
    UserAccountRepository u,
    BCryptPasswordEncoder e,
    FindByIndexNameSessionRepository<? extends Session> s
  ) {
    jdbc = j;
    users = u;
    encoder = e;
    sessions = s;
  }

  public boolean enabled() {
    return !host.isBlank() && !from.isBlank();
  }

  static String hash(String token) {
    try {
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(
          token.getBytes(StandardCharsets.UTF_8)
        )
      );
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public void request(String email, String ip) {
    if (!enabled()) throw new ResponseStatusException(
      HttpStatus.SERVICE_UNAVAILABLE,
      "Email recovery is not configured. Contact your administrator."
    );
    long now = System.currentTimeMillis();
    Long last = lastRequest.put(ip, now);
    if (last != null && last > now - 60000) throw new ResponseStatusException(
      HttpStatus.TOO_MANY_REQUESTS,
      "Please wait before requesting another link."
    );
    var user = users.findByEmailIgnoreCase(email.trim()).filter(u -> u.active);
    if (user.isEmpty()) return;
    var u = users.lock(user.get().id).orElseThrow();
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder()
      .withoutPadding()
      .encodeToString(bytes);
    jdbc.update(
      "UPDATE password_reset_token SET used=true WHERE user_id=?",
      u.id
    );
    jdbc.update(
      "INSERT INTO password_reset_token(token_hash,user_id,expires_at) VALUES(?,?,?)",
      hash(token),
      u.id,
      java.sql.Timestamp.from(Instant.now().plusSeconds(1800))
    );
    var sender = new JavaMailSenderImpl();
    sender.setHost(host);
    sender.setPort(port);
    sender.setUsername(username);
    sender.setPassword(password);
    var props = sender.getJavaMailProperties();
    props.setProperty("mail.smtp.auth", String.valueOf(!username.isBlank()));
    props.setProperty("mail.smtp.starttls.enable", "true");
    props.setProperty("mail.smtp.starttls.required", "true");
    props.setProperty("mail.smtp.connectiontimeout", "5000");
    props.setProperty("mail.smtp.timeout", "5000");
    var message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(u.email);
    message.setSubject("Reset your LogiCore password");
    message.setText(
      "Use this single-use link within 30 minutes:\n" +
        publicUrl +
        "/reset-password#" +
        token +
        "\nIf you did not request it, ignore this email."
    );
    sender.send(message);
  }

  @Transactional
  public void reset(String token, String password) {
    if (
      password.length() < 12 ||
      password.length() > 64 ||
      password.getBytes(StandardCharsets.UTF_8).length > 72
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Use 12–64 characters, at most 72 UTF-8 bytes."
    );
    var rows = jdbc.queryForList(
      "SELECT user_id FROM password_reset_token WHERE token_hash=? AND used=false AND expires_at>now() FOR UPDATE",
      hash(token)
    );
    if (rows.isEmpty()) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "The recovery link is invalid or expired."
    );
    long id = ((Number) rows.getFirst().get("user_id")).longValue();
    var u = users
      .lock(id)
      .filter(x -> x.active)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "The recovery link is invalid or expired."
        )
      );
    u.passwordHash = encoder.encode(password);
    users.save(u);
    jdbc.update(
      "UPDATE password_reset_token SET used=true WHERE user_id=?",
      id
    );
    sessions
      .findByPrincipalName(u.email)
      .keySet()
      .forEach(sessions::deleteById);
  }
}
