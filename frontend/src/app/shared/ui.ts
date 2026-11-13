import {
  Component,
  Directive,
  Input,
  HostBinding,



} from "@angular/core";


@Directive({ selector: "button[lcButton],a[lcButton]", standalone: true })
export class ButtonDirective {
  @Input() variant = "primary";
  @HostBinding("class") get classes() {
    return "btn btn-" + this.variant;
  }
}
@Component({
  selector: "lc-icon",
  standalone: true,
  template: `<svg
    viewBox="0 0 24 24"
    width="20"
    height="20"
    fill="none"
    stroke="currentColor"
    stroke-width="1.6"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path [attr.d]="paths[name] || paths['box']" />
  </svg>`,
})
export class Icon {
  @Input() name = "box";
  paths: Record<string, string> = {
    dashboard: "M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z",
    truck:
      "M3 5h12v12H3z M15 9h4l3 4v4h-7 M5 17a2 2 0 1 0 4 0 M16 17a2 2 0 1 0 4 0",
    file: "M6 2h8l5 5v15H6z M14 2v6h5 M9 12h7 M9 16h7",
    box: "M3 6l9-4 9 4v12l-9 4-9-4z M3 6l9 5 9-5 M12 11v11",
    users:
      "M16 21v-3a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v3 M9 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M17 4a4 4 0 0 1 0 7 M22 21v-3a4 4 0 0 0-3-4",
    chart: "M4 3v18h18 M8 16v-4 M13 16V7 M18 16v-6",
    shield: "M12 2l9 4v6c0 5-9 10-9 10S3 17 3 12V6z M8 12l3 3 5-6",
    settings:
      "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M12 2v3 M12 19v3 M2 12h3 M19 12h3 M5 5l2 2 M17 17l2 2 M5 19l2-2 M17 7l2-2",
    search: "M10 3a7 7 0 1 0 0 14 7 7 0 0 0 0-14 M15 15l6 6",
    plus: "M12 5v14 M5 12h14",
    arrow: "M4 12h16 M14 6l6 6-6 6",
    menu: "M4 6h16 M4 12h16 M4 18h16",
    sun: "M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8 M12 2v2 M12 20v2 M2 12h2 M20 12h2",
    logout: "M9 4H3v16h6 M8 12h13 M16 7l5 5-5 5",
    close: "M6 6l12 12 M6 18L18 6",
    download: "M12 3v12 M7 10l5 5 5-5 M4 16v5h16v-5",
    check: "M5 12l4 4L19 6",
  };
}
@Component({
  selector: "lc-badge",
  standalone: true,
  template:
    '<span class="badge" [attr.data-status]="value">{{value.replaceAll("_"," ")}}</span>',
})
export class StatusBadge {
  @Input() value = "";
}
@Component({
  selector: "lc-empty",
  imports: [Icon],
  template:
    '<div class="empty"><lc-icon name="box"/><h3>{{title}}</h3><p>{{description}}</p></div>',
})
export class EmptyState {
  @Input() title = "Nothing here yet";
  @Input() description =
    "Create your first record to start building your workspace.";
}
@Component({
  selector: "lc-loading",
  template:
    '<div class="skeleton" role="status" aria-label="Loading records"><div></div><div></div><div></div></div>',
})
export class LoadingState {}

