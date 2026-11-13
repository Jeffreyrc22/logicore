import {
  AfterViewInit,
  Directive,
  ElementRef,
  HostListener,
  OnDestroy,
  inject,
} from "@angular/core";
/** Keeps keyboard navigation inside a dialog and restores its trigger on close. */
@Directive({ selector: "[lcDialog]", standalone: true })
export class DialogFocus implements AfterViewInit, OnDestroy {
  private element = inject<ElementRef<HTMLElement>>(ElementRef);
  private previous = document.activeElement as HTMLElement | null;
  private controls() {
    return Array.from(
      this.element.nativeElement.querySelectorAll<HTMLElement>(
        'button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex="0"]',
      ),
    ).filter((el) => el.getClientRects().length > 0);
  }
  ngAfterViewInit() {
    queueMicrotask(() => this.controls()[0]?.focus());
  }
  @HostListener("keydown", ["$event"]) onKey(event: KeyboardEvent) {
    if (event.key !== "Tab") return;
    const items = this.controls();
    if (!items.length) {
      event.preventDefault();
      return;
    }
    const first = items[0],
      last = items[items.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      last.focus();
      event.preventDefault();
    } else if (!event.shiftKey && document.activeElement === last) {
      first.focus();
      event.preventDefault();
    }
  }
  ngOnDestroy() {
    if (this.previous?.isConnected) this.previous.focus();
  }
}
