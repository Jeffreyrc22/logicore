import { Component, inject } from "@angular/core";
import { bootstrapApplication } from "@angular/platform-browser";
import {
  provideRouter,
  RouterOutlet,
  Router,
  Routes,
  CanActivateFn,
} from "@angular/router";
import { provideHttpClient } from "@angular/common/http";
import { Api } from "./app/core/api";
import { Shell } from "./app/layouts/shell";
const authenticated: CanActivateFn = async () => {
  const api = inject(Api),
    router = inject(Router);
  return (await api.session()) ? true : router.createUrlTree(["/login"]);
};
const permission: CanActivateFn = (route) => {
  const api = inject(Api),
    router = inject(Router);
  return !route.data["roles"] || route.data["roles"].includes(api.user()?.role)
    ? true
    : router.createUrlTree(["/dashboard"]);
};
const resources = [
  "operations",
  "documents",
  "inventory",
  "customers",
  "suppliers",
  "users",
  "audit",
].map((kind) => ({
  path: kind,
  loadComponent: () =>
    import("./app/features/resources/resource").then((m) => m.ResourcePage),
  canActivate: [permission],
  data: {
    kind,
    roles: ["users", "audit"].includes(kind)
      ? ["SUPER_ADMIN"]
      : ["inventory", "customers", "suppliers"].includes(kind)
        ? ["SUPER_ADMIN", "MANAGER"]
        : undefined,
  },
}));
const routes: Routes = [
  {
    path: "recovery",
    loadComponent: () =>
      import("./app/features/auth/recovery").then((m) => m.Recovery),
  },
  {
    path: "reset-password",
    loadComponent: () =>
      import("./app/features/auth/recovery").then((m) => m.Recovery),
  },
  {
    path: "login",
    loadComponent: () =>
      import("./app/features/auth/login").then((m) => m.Login),
  },
  {
    path: "",
    component: Shell,
    canActivate: [authenticated],
    children: [
      { path: "", pathMatch: "full", redirectTo: "dashboard" },
      {
        path: "dashboard",
        loadComponent: () =>
          import("./app/features/dashboard/dashboard").then((m) => m.Dashboard),
      },
      ...resources,
      {
        path: "reports",
        canActivate: [permission],
        data: { roles: ["MANAGER", "SUPER_ADMIN"] },
        loadComponent: () =>
          import("./app/features/reports/reports").then((m) => m.Reports),
      },
      {
        path: "settings",
        canActivate: [permission],
        data: { roles: ["SUPER_ADMIN"] },
        loadComponent: () =>
          import("./app/features/settings/settings").then((m) => m.Settings),
      },
    ],
  },
  { path: "**", redirectTo: "dashboard" },
];
@Component({
  selector: "lc-app",
  imports: [RouterOutlet],
  template: "<router-outlet/>",
})
class App {}
bootstrapApplication(App, {
  providers: [provideHttpClient(), provideRouter(routes)],
}).catch(console.error);
