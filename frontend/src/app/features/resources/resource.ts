import { DialogFocus } from "../../shared/dialog-focus";
import { Component, inject, signal, OnDestroy } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
  FormsModule,
} from "@angular/forms";
import { Api } from "../../core/api";
import {
  ButtonDirective,
  Icon,
  StatusBadge,
  EmptyState,
  LoadingState,
} from "../../shared/ui";
interface Field {
  key: string;
  label: string;
  type?: string;
  required?: boolean;
  options?: { value: any; label: string }[];
  max?: number;
}
const options = (values: string[]) =>
  values.map((value) => ({ value, label: value.replaceAll("_", " ") }));
const transitions: Record<string, string[]> = {
  DRAFT: ["PENDING", "CANCELLED"],
  PENDING: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["IN_TRANSIT", "CANCELLED"],
  IN_TRANSIT: ["DELIVERED", "CANCELLED"],
  DELIVERED: ["COMPLETED"],
  COMPLETED: [],
  CANCELLED: [],
};
const documentTypes = [
  "INVOICE",
  "SHIPPING_DOCUMENT",
  "DELIVERY_RECEIPT",
  "INVENTORY_REPORT",
  "CONTRACT",
  "OTHER",
];
@Component({
  selector: "lc-resource",
  imports: [
    DialogFocus,
    ReactiveFormsModule,
    FormsModule,
    ButtonDirective,
    Icon,
    StatusBadge,
    EmptyState,
    LoadingState,
  ],
  templateUrl: "./resource.html",
})
export class ResourcePage implements OnDestroy {
  api = inject(Api);
  route = inject(ActivatedRoute);
  router = inject(Router);
  fb = inject(FormBuilder);
  kind = "";
  title = "";
  subtitle = "";
  endpoint = "";
  icon = "box";
  columns: { key: string; label: string }[] = [];
  fields: Field[] = [];
  rows = signal<any[]>([]);
  total = signal(0);
  pages = signal(0);
  page = 0;
  q = "";
  status = "";
  type = "";
  low = false;
  busy = signal(false);
  saving = signal(false);
  error = signal("");
  toast = signal("");
  dialog = signal(false);
  selected = signal<any>(null);
  detail = signal<any>(null);
  history = signal<any[]>([]);
  associatedDocs = signal<any[]>([]);
  form: FormGroup = this.fb.group({});
  file?: File;
  confirm = signal<{ title: string; run: () => Promise<void> } | null>(null);
  movement = signal<any>(null);
  delta = 1;
  reason = "";
  timer?: ReturnType<typeof setTimeout>;
  subscription = this.route.data.subscribe((data) => {
    this.kind = data["kind"];
    this.configure();
    const p = this.route.snapshot.queryParamMap;
    this.q = p.get("q") || "";
    this.status = p.get("status") || "";
    this.type = p.get("type") || "";
    this.page = Number(p.get("page") || 0);
    this.load();
  });
  ngOnDestroy() {
    this.subscription.unsubscribe();
    clearTimeout(this.timer);
  }
  configure() {
    this.endpoint = ["users", "audit"].includes(this.kind)
      ? "/admin/" + (this.kind === "users" ? "users" : "audit")
      : "/" + this.kind;
    const captions: Record<string, string[]> = {
      operations: [
        "Operations",
        "Every shipment, organized from start to finish.",
        "truck",
      ],
      documents: [
        "Documents",
        "The right information. Right where you need it.",
        "file",
      ],
      inventory: [
        "Inventory",
        "Stay in control of every item and every movement.",
        "box",
      ],
      customers: [
        "Customers",
        "Strong relationships start with a clear picture.",
        "users",
      ],
      suppliers: [
        "Suppliers",
        "Your partners, connected to your workflow.",
        "box",
      ],
      users: [
        "Team & access",
        "The right people. The right permissions.",
        "shield",
      ],
      audit: [
        "Activity log",
        "A traceable record of important workspace changes.",
        "file",
      ],
    };
    [this.title, this.subtitle, this.icon] = captions[this.kind];
    const cols: Record<string, string[]> = {
      operations: [
        "reference",
        "type",
        "origin",
        "destination",
        "estimatedDate",
        "status",
      ],
      documents: [
        "filename",
        "type",
        "operationId",
        "size",
        "createdAt",
        "status",
      ],
      inventory: [
        "sku",
        "name",
        "category",
        "quantity",
        "minimumStock",
        "location",
      ],
      customers: ["name", "company", "email", "phone", "active"],
      suppliers: ["name", "company", "category", "email", "active"],
      users: ["name", "email", "role", "active", "lastLogin"],
      audit: ["createdAt", "actorId", "action", "entity", "entityId", "detail"],
    };
    this.columns = cols[this.kind].map((key) => ({
      key,
      label:
        (
          {
            estimatedDate: "Estimated arrival",
            operationId: "Operation",
            createdAt: "Created",
            minimumStock: "Minimum",
            actorId: "User",
            entityId: "Record",
            lastLogin: "Last login",
          } as any
        )[key] || key.charAt(0).toUpperCase() + key.slice(1),
    }));
  }
  async load() {
    this.busy.set(true);
    this.error.set("");
    try {
      const p = new URLSearchParams({
        q: this.q,
        page: String(this.page),
        size: "12",
      });
      if (this.status) p.set("status", this.status);
      if (this.type) p.set("type", this.type);
      if (this.low) p.set("low", "true");
      const d = await this.api.request(this.endpoint + "?" + p);
      this.rows.set(d.content);
      this.total.set(d.totalElements);
      this.pages.set(d.totalPages);
    } catch (e: any) {
      this.error.set(e.message);
    } finally {
      this.busy.set(false);
    }
  }
  filter() {
    this.page = 0;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.q || null,
        status: this.status || null,
        type: this.type || null,
        page: null,
      },
      replaceUrl: true,
    });
    this.load();
  }
  paginate(delta: number) {
    this.page += delta;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: this.page },
      queryParamsHandling: "merge",
      replaceUrl: true,
    });
    this.load();
  }
  canEdit() {
    return (
      this.kind !== "audit" &&
      (!["customers", "suppliers", "inventory"].includes(this.kind) ||
        this.api.user()?.role !== "OPERATOR")
    );
  }
  cell(row: any, key: string) {
    const v = row[key];
    if (v === null || v === undefined) return "—";
    if (key === "size") return (v / 1024).toFixed(1) + " KB";
    if (key === "createdAt" || key === "lastLogin")
      return new Date(v).toLocaleString("en-US", {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    if (typeof v === "boolean") return v ? "Active" : "Inactive";
    return v;
  }
  notify(text: string) {
    this.toast.set(text);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.toast.set(""), 4000);
  }
  async open(row: any = null) {
    this.error.set("");
    this.selected.set(row);
    this.file = undefined;
    const text = (
      key: string,
      label: string,
      required = true,
      type = "text",
    ): Field => ({ key, label, required, type });
    try {
      if (this.kind === "operations") {
        const [customers, assignees] = await Promise.all([
          this.api.request("/customers?size=100&active=true"),
          this.api.request("/assignees"),
        ]);
        this.fields = [
          text("reference", "Reference number"),
          {
            ...text("type", "Operation type"),
            options: options([
              "ROAD_FREIGHT",
              "AIR_FREIGHT",
              "SEA_FREIGHT",
              "WAREHOUSE",
              "OTHER",
            ]),
          },
          {
            ...text("customerId", "Customer"),
            options: customers.content.map((c: any) => ({
              value: c.id,
              label: c.name,
            })),
          },
          text("origin", "Origin"),
          text("destination", "Destination"),
          text("estimatedDate", "Estimated arrival", true, "date"),
          {
            ...text("assignedId", "Assigned operator"),
            options: assignees.map((a: any) => ({
              value: a.id,
              label: a.name,
            })),
          },
          text("description", "Description", false, "textarea"),
        ];
      } else if (["customers", "suppliers"].includes(this.kind))
        this.fields = [
          text("name", "Contact name"),
          text("company", "Company", false),
          text("email", "Email", false, "email"),
          text("phone", "Phone", false),
          text("address", "Address", false),
          text("category", "Category", false),
          text("active", "Active", false, "checkbox"),
        ];
      else if (this.kind === "inventory")
        this.fields = [
          text("sku", "SKU"),
          text("name", "Item name"),
          text("description", "Description", false, "textarea"),
          text("category", "Category"),
          text("minimumStock", "Minimum stock", true, "number"),
          text("unit", "Unit"),
          text("location", "Warehouse location"),
          text("active", "Active", false, "checkbox"),
        ];
      else if (this.kind === "users")
        this.fields = [
          text("name", "Full name"),
          text("email", "Email", true, "email"),
          {
            ...text("role", "Role"),
            options: options(["OPERATOR", "MANAGER", "SUPER_ADMIN"]),
          },
          text(
            "password",
            row ? "New password (leave blank to keep)" : "Initial password",
            !row,
            "password",
          ),
          text("active", "Active", false, "checkbox"),
        ];
      else if (this.kind === "documents") {
        const ops = await this.api.request("/operations?size=100");
        this.fields = [
          ...(!row
            ? [
                {
                  ...text("operationId", "Associated operation"),
                  options: ops.content.map((o: any) => ({
                    value: o.id,
                    label: o.reference,
                  })),
                },
              ]
            : []),
          { ...text("type", "Document type"), options: options(documentTypes) },
          text("description", "Description", false, "textarea"),
          ...(row
            ? [
                {
                  ...text("status", "Status"),
                  options: options(
                    this.api.user()?.role === "OPERATOR"
                      ? [row.status]
                      : ["ACTIVE", "ARCHIVED"],
                  ),
                },
              ]
            : []),
        ];
      }
      const controls: Record<string, any> = {};
      for (const f of this.fields) {
        const value =
          row?.[f.key] ??
          (f.type === "checkbox" ? true : f.type === "number" ? 0 : "");
        const validators = f.required ? [Validators.required] : [];
        if (f.type === "email") validators.push(Validators.email);
        if (f.type === "password")
          validators.push(Validators.minLength(12), Validators.maxLength(64));
        if (f.type === "number") validators.push(Validators.min(0));
        controls[f.key] = [value, validators];
      }
      this.form = this.fb.group(controls);
      this.dialog.set(true);
    } catch (e: any) {
      this.error.set(e.message);
    }
  }
  async save() {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    if (this.kind === "documents" && !this.selected() && !this.file) {
      this.error.set("Choose a document to upload.");
      return;
    }
    this.saving.set(true);
    this.error.set("");
    try {
      const row = this.selected();
      const value = { ...this.form.getRawValue(), version: row?.version || 0 };
      for (const k of ["customerId", "assignedId", "minimumStock"])
        if (k in value) value[k] = Number(value[k]);
      let body: any = value;
      if (this.kind === "documents" && !row) {
        body = new FormData();
        for (const k of ["operationId", "type", "description"])
          body.append(k, String(value[k] || ""));
        body.append("file", this.file!);
      }
      await this.api.request(
        this.endpoint + (row ? "/" + row.id : ""),
        row ? "PUT" : "POST",
        body,
      );
      this.dialog.set(false);
      this.notify(row ? "Changes saved." : "Record created.");
      await this.load();
    } catch (e: any) {
      this.error.set(e.message);
    } finally {
      this.saving.set(false);
    }
  }
  pickFile(event: Event) {
    this.file = (event.target as HTMLInputElement).files?.[0];
  }
  async view(row: any) {
    this.detail.set(row);
    this.history.set([]);
    this.associatedDocs.set([]);
    try {
      if (this.kind === "operations") {
        const [h, d] = await Promise.all([
          this.api.request("/operations/" + row.id + "/history"),
          this.api.request("/documents?operationId=" + row.id + "&size=100"),
        ]);
        this.history.set(h);
        this.associatedDocs.set(d.content);
      }
      if (this.kind === "inventory")
        this.history.set(
          await this.api.request("/inventory/" + row.id + "/movements"),
        );
    } catch (e: any) {
      this.error.set(e.message);
    }
  }
  next(row: any) {
    return (transitions[row.status] || []).filter(
      (s) => s !== "CANCELLED" || this.api.user()?.role !== "OPERATOR",
    );
  }
  changeStatus(row: any, event: Event) {
    const status = (event.target as HTMLSelectElement).value;
    if (!status) return;
    this.confirm.set({
      title:
        "Move " + row.reference + " to " + status.replaceAll("_", " ") + "?",
      run: async () => {
        await this.api.request("/operations/" + row.id + "/status", "PATCH", {
          status,
        });
        await this.load();
        this.detail.set(null);
        this.notify("Operation status updated.");
      },
    });
    (event.target as HTMLSelectElement).value = "";
  }
  async confirmAction() {
    const c = this.confirm();
    if (!c) return;
    this.saving.set(true);
    try {
      await c.run();
      this.confirm.set(null);
    } catch (e: any) {
      this.error.set(e.message);
    } finally {
      this.saving.set(false);
    }
  }
  async saveMovement() {
    if (
      !this.reason.trim() ||
      !Number.isInteger(Number(this.delta)) ||
      !Number(this.delta)
    ) {
      this.error.set("Enter a non-zero whole number and a reason.");
      return;
    }
    this.saving.set(true);
    try {
      await this.api.request(
        "/inventory/" + this.movement().id + "/movements",
        "POST",
        { delta: Number(this.delta), reason: this.reason },
      );
      this.movement.set(null);
      this.reason = "";
      this.notify("Stock movement recorded.");
      await this.load();
    } catch (e: any) {
      this.error.set(e.message);
    } finally {
      this.saving.set(false);
    }
  }
}
