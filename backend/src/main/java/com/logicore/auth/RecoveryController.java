package com.logicore.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class RecoveryController {

  private final RecoveryService service;

  public RecoveryController(RecoveryService s) {
    service = s;
  }

  record Request(@Email @NotBlank String email) {}

  record Reset(
    @NotBlank @Size(max = 100) String token,
    @NotBlank @Size(min = 12, max = 64) String password
  ) {}

  @GetMapping("/capabilities")
  public Object capabilities() {
    return Map.of("emailRecovery", service.enabled());
  }

  @PostMapping("/recovery")
  public Object request(
    @Valid @RequestBody Request in,
    HttpServletRequest req
  ) {
    service.request(in.email(), req.getRemoteAddr());
    return Map.of(
      "message",
      "If an active account matches, a recovery link will be sent."
    );
  }

  @PostMapping("/reset-password")
  public Object reset(@Valid @RequestBody Reset in) {
    service.reset(in.token(), in.password());
    return Map.of(
      "message",
      "Password updated. Sign in with your new password."
    );
  }
}
