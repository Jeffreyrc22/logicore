package com.logicore.shared;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
class SpaController {
  @GetMapping({"/", "/login", "/recovery", "/reset-password", "/dashboard", "/operations", "/documents", "/inventory", "/customers", "/suppliers", "/users", "/audit", "/reports", "/settings"})
  String app() { return "forward:/index.html"; }
}
