package com.logicore.auth;

import com.logicore.operations.Operation;
import com.logicore.users.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class Access {

  private final UserAccountRepository users;

  public Access(UserAccountRepository u) {
    users = u;
  }

  public UserAccount user() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a == null) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Sign in to continue."
    );
    return users
      .findByEmailIgnoreCase(a.getName())
      .filter(u -> u.active)
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Session unavailable."
        )
      );
  }

  public void manager() {
    if (user().role.equals("OPERATOR")) deny();
  }

  public void admin() {
    if (!user().role.equals("SUPER_ADMIN")) deny();
  }

  public void operation(Operation o) {
    var u = user();
    if (u.role.equals("OPERATOR") && !u.id.equals(o.assignedId)) deny();
  }

  public static void deny() {
    throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "This action is not permitted."
    );
  }
}
