import { Component, inject, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { Api } from "../../core/api";
import { ButtonDirective, Icon } from "../../shared/ui";
@Component({
  selector: "lc-login",
  imports: [ReactiveFormsModule, ButtonDirective, Icon, RouterLink],
  template: `<div class="login-page">
    <section class="login-art">
      <a class="brand"><span class="brand-mark">✣</span> LogiCore</a>
      <div>
        <span class="eyebrow">ONE WORKSPACE. EVERY MOVEMENT.</span>
        <h1>Operations,<br />beautifully<br /><em>connected.</em></h1>
        <p>
          Your logistics, documents and people.<br />A clearer view of
          everything that moves.
        </p>
      </div>
      <small>LOGISTICS & DOCUMENT MANAGEMENT</small>
    </section>
    <section class="login-form">
      <span class="eyebrow">WELCOME BACK</span>
      <h2>Sign in to your workspace.</h2>
      <p class="muted">Keep your operations moving forward.</p>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <label
          >Email address<input
            type="email"
            formControlName="email"
            autocomplete="username"
            placeholder="you@company.com" /></label
        ><label
          >Password<input
            type="password"
            formControlName="password"
            autocomplete="current-password"
        /></label>
        @if (error()) {
          <p class="alert error" role="alert">{{ error() }}</p>
        }
        <button lcButton type="submit" [disabled]="busy() || form.invalid">
          {{ busy() ? "Signing in…" : "Sign in" }} <lc-icon name="arrow" />
        </button>
      </form>
      <p class="login-note">
        Need access? Contact your workspace administrator.<br /><a
          routerLink="/recovery"
          >Account recovery options →</a
        >
      </p>
    </section>
  </div>`,
})
export class Login {
  api = inject(Api);
  router = inject(Router);
  fb = inject(FormBuilder);
  form = this.fb.nonNullable.group({
    email: ["", [Validators.required, Validators.email]],
    password: ["", Validators.required],
  });
  busy = signal(false);
  error = signal("");
  async submit() {
    if (this.form.invalid) return;
    this.busy.set(true);
    this.error.set("");
    try {
      const v = this.form.getRawValue();
      await this.api.login(v.email, v.password);
      await this.router.navigateByUrl("/dashboard");
    } catch (e: any) {
      this.error.set(e.message);
    } finally {
      this.busy.set(false);
    }
  }
}
