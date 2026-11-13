import { Component, Input, ElementRef, viewChild, OnDestroy } from "@angular/core"; import { Chart, registerables } from "chart.js"; Chart.register(...registerables);
@Component({
  selector: "lc-chart",
  template:
    '<div class="chart-canvas"><canvas #canvas [attr.aria-label]="label" role="img"></canvas></div>',
})
export class ChartComponent implements OnDestroy {
  @Input() label = "Operations";
  @Input() data: Record<string, number> = {};
  @Input() type: "bar" | "doughnut" = "bar";
  canvas = viewChild<ElementRef<HTMLCanvasElement>>("canvas");
  chart?: Chart;
  ngAfterViewInit() {
    this.render();
  }
  ngOnChanges() {
    this.render();
  }
  render() {
    if (!this.canvas()) return;
    this.chart?.destroy();
    this.chart = new Chart(this.canvas()!.nativeElement, {
      type: this.type,
      data: {
        labels: Object.keys(this.data).map((k) => k.replaceAll("_", " ")),
        datasets: [
          {
            label: this.label,
            data: Object.values(this.data),
            backgroundColor:
              this.type === "bar"
                ? "#ff8a43"
                : [
                    "#ff8a43",
                    "#52c7bc",
                    "#708ce4",
                    "#d8b879",
                    "#9a81cb",
                    "#637184",
                    "#df6d78",
                  ],
            borderRadius: this.type === "bar" ? 6 : 0,
          } as any,
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: this.type === "doughnut",
            position: "bottom",
            labels: { color: "#8c96a6", boxWidth: 10, padding: 18 },
          },
        },
        scales:
          this.type === "bar"
            ? {
                x: { grid: { display: false }, ticks: { color: "#8c96a6" } },
                y: {
                  beginAtZero: true,
                  ticks: { precision: 0, color: "#8c96a6" },
                  grid: { color: "#8899aa18" },
                },
              }
            : undefined,
      },
    });
  }
  ngOnDestroy() {
    this.chart?.destroy();
  }
}

