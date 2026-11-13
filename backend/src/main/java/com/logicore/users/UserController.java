package com.logicore.users;

import com.logicore.auth.Access;
import com.logicore.shared.Queries;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {

  private final UserAccountRepository repo;
  private final UserService service;
  private final Access access;

  public UserController(UserAccountRepository r, UserService s, Access a) {
    repo = r;
    service = s;
    access = a;
  }

  @GetMapping("/assignees")
  public Object assignees() {
    var me = access.user();
    return (
      me.role.equals("OPERATOR")
        ? List.of(me)
        : repo
            .findAll()
            .stream()
            .filter(u -> u.active)
            .toList()
    )
      .stream()
      .map(u -> Map.of("id", u.id, "name", u.name, "role", u.role))
      .toList();
  }

  @GetMapping("/admin/users")
  public Object list(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "12") int size
  ) {
    access.admin();
    return repo.findAll(
      Queries.search(q, "name", "email"),
      Queries.page(page, size)
    );
  }

  @PostMapping("/admin/users")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public UserAccount create(@Valid @RequestBody UserService.Input in) {
    return service.save(null, in);
  }

  @PutMapping("/admin/users/{id}")
  public UserAccount edit(
    @PathVariable Long id,
    @Valid @RequestBody UserService.Input in
  ) {
    return service.save(id, in);
  }
}
