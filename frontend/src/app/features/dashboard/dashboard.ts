import { ChartComponent } from "../../shared/chart";
import { Component, inject, signal } from "@angular/core";
import { RouterLink } from "@angular/router";
import { Api } from "../../core/api";
import {
  Icon,
  ButtonDirective,
  EmptyState,
  LoadingState,

} from "../../shared/ui";
@Component({
  selector: "lc-dashboard",
  imports: [
    RouterLink,
    Icon,
    ButtonDirective,
    EmptyState,
    LoadingState,
    ChartComponent,
  ],
  template: `<div class="page-heading">
      <div>
        <span class="eyebrow">YOUR OPERATIONS AT A GLANCE</span>
        <h1>
          Welcome back, {{ api.user()?.name?.split(" ")?.[0]
          }}<span class="orange">.</span>
        </h1>
        <p>A clearer view of what’s moving, and what needs you.</p>
      </div>
      <a lcButton routerLink="/operations"
        ><lc-icon name="plus" /> New operation</a
      >
    </div>
    @if (error()) {
      <p class="alert error">{{ error() }}</p>
    }
    @if (!data()) {
      <lc-loading />
    } @else {
      <div class="kpi-grid">
        @for (k of kpis; track k.key) {
          @if (k.key !== "inventoryAlerts" || api.user()?.role !== "OPERATOR") {
            <article class="kpi">
              <div class="kpi-top">
                <lc-icon [name]="k.icon" /><span class="kpi-source">LIVE</span>
              </div>
              <strong>{{ data()[k.key] }}</strong
              ><span>{{ k.label }}</span>
            </article>
          }
        }
      </div>
      <div class="dashboard-grid">
        <article class="panel trend">
          <div class="panel-heading">
            <div>
              <h2>Operations overview</h2>
              <p>Monthly activity across your authorized operations</p>
            </div>
            <span class="subtle-badge">ALL TIME</span>
          </div>
          @if (data().totalOperations) {
            <lc-chart [data]="data().byMonth" label="Operations" />
          } @else {
            <lc-empty
              title="No operations yet"
              description="Create an operation to see activity and trends in this space."
            />
          }
          <a routerLink="/operations" class="inline-link"
            >View operations <lc-icon name="arrow"
          /></a>
        </article>
        <article class="panel">
          <div class="panel-heading">
            <div>
              <h2>Shipment status</h2>
              <p>Every stage, in focus</p>
            </div>
            <lc-icon name="truck" />
          </div>
          @if (data().totalOperations) {
            <lc-chart [data]="data().byStatus" type="doughnut" label="Status" />
          } @else {
            <lc-empty
              title="No shipments yet"
              description="Status distribution appears as your team adds operations."
            />
          }
        </article>
        <article class="panel">
          <div class="panel-heading">
            <div>
              <h2>Recent activity</h2>
              <p>The latest updates in your workflow</p>
            </div>
            <lc-icon name="file" />
          </div>
          @for (event of data().recent; track event.id) {
            <div class="activity">
              <span class="activity-dot"></span>
              <div>
                <strong>{{ event.event.replaceAll("_", " ") }}</strong>
                <p>{{ event.detail }}</p>
              </div>
              <a [routerLink]="'/operations'" class="muted"
                >#{{ event.operationId }}</a
              >
            </div>
          }
          @if (!data().recent.length) {
            <lc-empty
              title="All quiet for now"
              description="Operation changes will appear here."
            />
          }
        </article>
        <article class="panel quick-panel">
          <span class="eyebrow">A PLACE FOR EVERY DETAIL</span
          ><lc-icon name="file" />
          <h2>Keep your documents<br />close to your operations.</h2>
          <p>
            Upload, organize and securely share access through each operation’s
            assigned team.
          </p>
          <a lcButton variant="secondary" routerLink="/documents"
            >Open documents <lc-icon name="arrow"
          /></a>
        </article>
      </div>
    }`,
})
export class Dashboard {
  api = inject(Api);
  data = signal<any>(null);
  error = signal("");
  kpis = [
    { key: "totalOperations", label: "Total operations", icon: "box" },
    { key: "activeShipments", label: "Active shipments", icon: "truck" },
    {
      key: "pendingDocuments",
      label: "Operations without documents",
      icon: "file",
    },
    {
      key: "completedDeliveries",
      label: "Delivered / completed",
      icon: "check",
    },
    { key: "inventoryAlerts", label: "Low stock alerts", icon: "box" },
    { key: "monthlyOperations", label: "Operations this month", icon: "chart" },
  ];
  constructor() {
    this.api
      .request("/dashboard")
      .then((d) => this.data.set(d))
      .catch((e) => this.error.set(e.message));
  }
}

