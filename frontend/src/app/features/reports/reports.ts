import { ChartComponent } from "../../shared/chart";
import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Api } from "../../core/api";
import {
  ButtonDirective,

  EmptyState,
  LoadingState,
  Icon,
} from "../../shared/ui";
@Component({
  selector: "lc-reports",
  imports: [
    FormsModule,
    ButtonDirective,
    ChartComponent,
    EmptyState,
    LoadingState,
    Icon,
  ],
  template: `<div class="page-heading">
      <div>
        <span class="eyebrow">FROM ACTIVITY TO INSIGHT</span>
        <h1>Reports & analytics<span class="orange">.</span></h1>
        <p>Real operational data. Better informed decisions.</p>
      </div>
      <div class="heading-actions">
        <a lcButton variant="secondary" [href]="csvUrl()"
          ><lc-icon name="download" />Export CSV</a
        ><button lcButton (click)="print()">Print / save PDF</button>
      </div>
    </div>
    <div class="panel report-filters">
      <label>From<input type="date" [(ngModel)]="from" /></label
      ><label>To<input type="date" [(ngModel)]="to" /></label
      ><button lcButton [disabled]="busy()" (click)="load()">
        Apply dates
      </button>
      <p class="muted small">
        Dates filter operation creation time (UTC). Inventory shows current
        balances.
      </p>
    </div>
    @if (error()) {
      <p class="alert error">{{ error() }}</p>
    }
    @if (busy()) {
      <lc-loading />
    } @else if (data()) {
      <div class="report-summary">
        <strong>{{ data().totalOperations }}</strong
        ><span>operations in the selected period</span
        ><strong>{{ data().completedDeliveries }}</strong
        ><span>delivered / completed</span>
      </div>
      <div class="dashboard-grid">
        @for (chart of charts; track chart.key) {
          <article class="panel">
            <h2>{{ chart.label }}</h2>
            @if (has(data()[chart.key])) {
              <lc-chart
                [data]="data()[chart.key]"
                [type]="chart.type"
                [label]="chart.label"
              />
            } @else {
              <lc-empty
                title="No data for this view"
                description="Records will appear when your workspace has matching activity."
              />
            }
          </article>
        }
      </div>
      <article class="panel">
        <h2>Inventory summary</h2>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>SKU</th>
                <th>Item</th>
                <th>Category</th>
                <th>Quantity</th>
                <th>Minimum</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              @for (i of inventory(); track i.id) {
                <tr>
                  <td>{{ i.sku }}</td>
                  <td>{{ i.name }}</td>
                  <td>{{ i.category }}</td>
                  <td>{{ i.quantity }}</td>
                  <td>{{ i.minimumStock }}</td>
                  <td [class.warning]="i.quantity <= i.minimumStock">
                    {{
                      i.quantity <= i.minimumStock ? "Low stock" : "Available"
                    }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        @if (!inventory().length) {
          <lc-empty title="No inventory records" />
        }
      </article>
    }`,
})
export class Reports {
  api = inject(Api);
  from = "";
  to = "";
  data = signal<any>(null);
  inventory = signal<any[]>([]);
  busy = signal(false);
  error = signal("");
  charts: { key: string; label: string; type: "bar" | "doughnut" }[] = [
    { key: "byMonth", label: "Monthly operations", type: "bar" },
    { key: "byStatus", label: "Operations by status", type: "doughnut" },
    { key: "inventoryByCategory", label: "Inventory by category", type: "bar" },
    { key: "documentSummary", label: "Document summary", type: "bar" },
  ];
  constructor() {
    this.load();
  }
  query() {
    return (
      "?" +
      new URLSearchParams({
        ...(this.from ? { from: this.from } : {}),
        ...(this.to ? { to: this.to } : {}),
      })
    );
  }
  csvUrl() {
    return "/api/v1/reports/operations.csv" + this.query();
  }
  has(data: any) {
    return Object.keys(data || {}).length > 0;
  }
  async load() {
    if (this.from && this.to && this.from > this.to) {
      this.error.set("The start date must precede the end date.");
      return;
    }
    this.busy.set(true);
    this.error.set("");
    try {
      const [d, i] = await Promise.all([
        this.api.request("/reports/operations" + this.query()),
        this.api.request("/reports/inventory"),
      ]);
      this.data.set(d);
      this.inventory.set(i);
    } catch (e: any) {
      this.error.set(e.message);
    } finally {
      this.busy.set(false);
    }
  }
  print() {
    window.print();
  }
}

