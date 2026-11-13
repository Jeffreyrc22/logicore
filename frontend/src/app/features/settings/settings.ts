import { Component, inject, signal } from "@angular/core";
import { FormBuilder, ReactiveFormsModule, Validators } from "@angular/forms";
import { Api } from "../../core/api";
import { ButtonDirective } from "../../shared/ui";
@Component({
  selector: "lc-settings",
  imports: [ReactiveFormsModule, ButtonDirective],
  template: `<div class="page-heading">
      <div>
        <span class="eyebrow">WORKSPACE PREFERENCES</span>
        <h1>Settings<span class="orange">.</span></h1>
        <p>Your company, your workspace.</p>
      </div>
    </div>
    <form class="panel settings-form" [formGroup]="form" (ngSubmit)="save()">
      <h2>Company profile</h2>
      <label
        >Company name<input
          formControlName="companyName"
          maxlength="160" /></label
      ><label
        >Time zone<input
          formControlName="timezone"
          placeholder="America/Bogota" /></label
      ><label
        >Support email<input type="email" formControlName="supportEmail"
      /></label>
      <p class="muted small">
        Stored company preferences. Operational timestamps and reports use UTC.
        This email is informational; mail delivery is not configured.
      </p>
      @if (message()) {
        <p class="alert" role="status">{{ message() }}</p>
      }
      @if (error()) {
        <p class="alert error" role="alert">{{ error() }}</p>
      }
      <button lcButton [disabled]="busy() || form.invalid">
        {{ busy() ? "Saving…" : "Save preferences" }}
      </button>
    </form>
    <section class="panel settings-form">
      <h2>Security & access</h2>
      <p>
        Sessions use HttpOnly cookies, server-side permissions and CSRF
        protection. Password resets can be performed privately by a super
        administrator in Team & access.
      </p>
      <p class="muted">
        Email recovery is unavailable until a mail service is configured.
      </p>
    </section>`,
})
export class Settings {
  api = inject(Api);
  fb = inject(FormBuilder);
  form = this.fb.nonNullable.group({
    companyName: ["", Validators.required],
    timezone: ["America/Bogota", Validators.required],
    supportEmail: ["", Validators.email],
  });
  busy = signal(false);
  message = signal("");
  error = signal("");
  constructor() {
    this.api
      .request("/admin/settings")
      .then((d) => this.form.patchValue(d))
      .catch((e) => this.error.set(e.message));
  }
  async save() {
    if (this.form.invalid) return;
    this.busy.set(true);
    this.error.set("");
    try {
      await this.api.request("/admin/settings", "PUT", this.form.getRawValue());
      this.message.set("Preferences saved.");
    } catch (e: any) {
      this.error.set(e.message);
    } finally {
      this.busy.set(false);
    }
  }
}
