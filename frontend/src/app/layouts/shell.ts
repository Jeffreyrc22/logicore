import { Component, inject, signal } from "@angular/core";
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from "@angular/router";
import { Api } from "../core/api";
import { Icon, ButtonDirective } from "../shared/ui";
@Component({
  selector: "lc-shell",
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon, ButtonDirective],
  template: `<div
    class="workspace"
    [class.light]="light()"
    [class.collapsed]="collapsed()"
  >
    <aside class="sidebar">
      <a routerLink="/dashboard" class="brand"
        ><span class="brand-mark">✣</span
        ><span class="brand-text"
          >LogiCore<span>OPERATIONS WORKSPACE</span></span
        ></a
      ><span class="nav-caption">WORKSPACE</span>
      <nav aria-label="Main navigation">
        @for (item of links; track item.path) {
          @if (allowed(item.roles)) {
            <a
              [routerLink]="item.path"
              routerLinkActive="active"
              [title]="item.label"
              ><lc-icon [name]="item.icon" /><span>{{ item.label }}</span></a
            >
          }
        }
      </nav>
      <div class="sidebar-bottom">
        <div class="workspace-card">
          <span class="live-dot"></span>
          <div>
            <strong>Connected workspace</strong
            ><small>Operations & documents</small>
          </div>
        </div>
        <button lcButton variant="ghost" (click)="logout()">
          <lc-icon name="logout" /><span>Sign out</span>
        </button>
      </div>
    </aside>
    <div class="main-wrap">
      <header class="topbar">
        <button
          lcButton
          variant="icon"
          aria-label="Toggle sidebar"
          (click)="collapsed.set(!collapsed())"
        >
          <lc-icon name="menu" />
        </button>
        <div class="breadcrumb">
          Workspace <span>/</span> <strong>{{ title() }}</strong>
        </div>
        <div class="topbar-end">
          <span class="today">{{ today }}</span
          ><button
            lcButton
            variant="icon"
            aria-label="Toggle color theme"
            (click)="toggleTheme()"
          >
            <lc-icon name="sun" /></button
          ><span class="avatar">{{ api.user()?.name?.slice(0, 1) }}</span>
          <div class="user-label">
            <strong>{{ api.user()?.name }}</strong
            ><small>{{ api.user()?.role?.replaceAll("_", " ") }}</small>
          </div>
        </div>
      </header>
      <main><router-outlet /></main>
      <footer class="app-footer">
        LogiCore <span>One workspace. Every movement.</span>
      </footer>
    </div>
  </div>`,
})
export class Shell {
  api = inject(Api);
  router = inject(Router);
  collapsed = signal(false);
  light = signal(localStorage.getItem("logicore-theme") === "light");
  today = new Date().toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
  links = [
    { path: "/dashboard", label: "Overview", icon: "dashboard", roles: [] },
    { path: "/operations", label: "Operations", icon: "truck", roles: [] },
    { path: "/documents", label: "Documents", icon: "file", roles: [] },
    {
      path: "/inventory",
      label: "Inventory",
      icon: "box",
      roles: ["MANAGER", "SUPER_ADMIN"],
    },
    {
      path: "/customers",
      label: "Customers",
      icon: "users",
      roles: ["MANAGER", "SUPER_ADMIN"],
    },
    {
      path: "/suppliers",
      label: "Suppliers",
      icon: "box",
      roles: ["MANAGER", "SUPER_ADMIN"],
    },
    {
      path: "/reports",
      label: "Reports & analytics",
      icon: "chart",
      roles: ["MANAGER", "SUPER_ADMIN"],
    },
    {
      path: "/users",
      label: "Team & access",
      icon: "shield",
      roles: ["SUPER_ADMIN"],
    },
    {
      path: "/audit",
      label: "Activity log",
      icon: "file",
      roles: ["SUPER_ADMIN"],
    },
    {
      path: "/settings",
      label: "Settings",
      icon: "settings",
      roles: ["SUPER_ADMIN"],
    },
  ];
  allowed(roles: string[]) {
    return !roles.length || roles.includes(this.api.user()?.role || "");
  }
  title() {
    return (
      this.links.find((x) => this.router.url.startsWith(x.path))?.label ||
      "Workspace"
    );
  }
  toggleTheme() {
    this.light.set(!this.light());
    localStorage.setItem("logicore-theme", this.light() ? "light" : "dark");
  }
  async logout() {
    try {
      await this.api.logout();
      this.router.navigateByUrl("/login");
    } catch {
      this.router.navigateByUrl("/login");
    }
  }
}

