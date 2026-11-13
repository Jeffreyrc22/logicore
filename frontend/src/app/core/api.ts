import { Injectable, inject, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { firstValueFrom } from "rxjs";
export interface User {
  id: number;
  name: string;
  email: string;
  role: string;
  active: boolean;
}
@Injectable({ providedIn: "root" })
export class Api {
  private http = inject(HttpClient);
  private csrf?: { token: string; headerName: string };
  user = signal<User | null>(null);
  async request<T = any>(
    path: string,
    method = "GET",
    body?: unknown,
  ): Promise<T> {
    try {
      if (!["GET", "HEAD"].includes(method) && !this.csrf)
        this.csrf = await firstValueFrom(
          this.http.get<{ token: string; headerName: string }>(
            "/api/v1/auth/csrf",
          ),
        );
      const headers: Record<string, string> = {};
      if (this.csrf && !["GET", "HEAD"].includes(method))
        headers[this.csrf.headerName] = this.csrf.token;
      const data = await firstValueFrom(
        this.http.request<T>(method, "/api/v1" + path, {
          body,
          headers,
          withCredentials: true,
        }),
      );
      if (path.startsWith("/auth/")) this.csrf = undefined;
      return data;
    } catch (e: any) {
      if (e.status === 401) this.user.set(null);
      throw new Error(
        e.error?.message || "Connection unavailable. Please try again.",
      );
    }
  }
  async session() {
    try {
      this.user.set(await this.request<User>("/auth/me"));
      return true;
    } catch {
      return false;
    }
  }
  async login(email: string, password: string) {
    this.user.set(
      await this.request<User>("/auth/login", "POST", { email, password }),
    );
  }
  async logout() {
    await this.request("/auth/logout", "POST");
    this.user.set(null);
  }
}
