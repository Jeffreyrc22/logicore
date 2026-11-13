import { Component, inject, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { Api } from "../../core/api";
import { ButtonDirective } from "../../shared/ui";
@Component({
  selector: "lc-recovery",
  imports: [ReactiveFormsModule, RouterLink, ButtonDirective],
  template: `<div class="login-page">
    <section class="login-art">
      <a routerLink="/login" class="brand"
        ><span class="brand-mark">✣</span> LogiCore</a
      >
      <div>
        <h1>Back to<br />your <em>workspace.</em></h1>
        <p>Secure access to the work that matters.</p>
      </div>
      <small>ACCOUNT RECOVERY</small>
    </section>
    <section class="login-form">
      <span class="eyebrow">ACCOUNT ACCESS</span>
      <h2>{{ reset ? "Choose a new password." : "Recover your account." }}</h2>
      @if (!reset && !available()) {
        <p class="alert">
          Email recovery is not configured. Contact your workspace
          administrator.
        </p>
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()">
          @if (reset) {
            <label
              >New password<input
                type="password"
                autocomplete="new-password"
                formControlName="password"
                minlength="12"
                maxlength="64" /></label
            ><label
              >Confirm password<input
                type="password"
                autocomplete="new-password"
                formControlName="confirm"
                minlength="12"
                maxlength="64"
            /></label>
            <p class="muted small">
              12–64 characters. Updating your password signs out existing
              sessions.
            </p>
          } @else {
            <label
              >Email address<input
                type="email"
                autocomplete="email"
                formControlName="email"
            /></label>
          }
          <button lcButton type="submit" [disabled]="busy() || done()">
            {{
              busy()
                ? "Please wait…"
                : reset
                  ? "Update password"
                  : "Request recovery link"
            }}
          </button>
        </form>
      }
      @if (error()) {
        <p class="alert error" role="alert">{{ error() }}</p>
      }
      @if (message()) {
        <p class="alert" role="status">{{ message() }}</p>
      }
      <a class="inline-link" routerLink="/login">Return to sign in →</a>
    </section>
  </div>`,
})
export class Recovery {
  api = inject(Api);
  route = inject(ActivatedRoute);
  fb = inject(FormBuilder);
  reset = this.route.snapshot.routeConfig?.path === "reset-password";
  token = this.route.snapshot.fragment || "";
  available = signal(false);
  busy = signal(false);
  done = signal(false);
  error = signal("");
  message = signal("");
  form = this.fb.nonNullable.group({
    email: [""],
    password: [""],
    confirm: [""],
  });
  constructor() {
    if (this.reset) {
      this.form.controls.password.addValidators([
        Validators.required,
        Validators.minLength(12),
        Validators.maxLength(64),
      ]);
      this.form.controls.confirm.addValidators(Validators.required);
      history.replaceState(
        history.state,
        "",
        location.pathname + location.search,
      );
    } else {
      this.form.controls.email.addValidators([
        Validators.required,
        Validators.email,
      ]);
      this.api
        .request("/auth/capabilities")
        .then((d) => this.available.set(d.emailRecovery))
        .catch((e) => this.error.set(e.message));
    }
  }
  async submit() {
    this.error.set("");
    this.form.updateValueAndValidity();
    const value = this.form.getRawValue();
    if (this.form.invalid) {
      this.error.set("Check the required fields.");
      return;
    }
    if (this.reset && value.password !== value.confirm) {
      this.error.set("Passwords must match.");
      return;
    }
    this.busy.set(true);
    try {
      const result = await this.api.request(
        this.reset ? "/auth/reset-password" : "/auth/recovery",
        "POST",
        this.reset
          ? { token: this.token, password: value.password }
          : { email: value.email },
      );
      this.message.set(result.message);
      this.done.set(true);
      this.form.reset();
    } catch (e: any) {
      this.error.set(e.message);
    } finally {
      this.busy.set(false);
    }
  }
}
