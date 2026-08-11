import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';
import { ButtonComponent } from '@shared/components/ui/button.component';
import { AlertEvent } from '@core/models/types';
import { relativeDate } from '@core/utils/utils';
import { formatMetric } from '@pages/alerts/utils/alert.utils';
import { AlertSeverityBadgeComponent } from './alert-severity-badge.component';

@Component({
  selector: 'app-alert-event-row',
  standalone: true,
  imports: [CommonModule, SvgIconComponent, ButtonComponent, AlertSeverityBadgeComponent],
  template: `
    <article class="rounded-lg border border-line bg-white p-3 shadow-card">
      <div class="flex items-start gap-2">
        <app-alert-severity-badge [severity]="alert.severity" [compact]="true"></app-alert-severity-badge>
        <div class="min-w-0 flex-1">
          <div class="flex gap-2">
            <h3 class="flex-1 text-xs font-semibold text-ink">{{ alert.ruleName }}</h3>
            <span class="text-2xs font-medium text-ink-faint">{{ statusLabel }}</span>
          </div>
          <p class="mt-1 text-xs text-ink-soft">{{ alert.message }}</p>
          <p class="mt-2 text-2xs text-ink-faint">
            {{ relativeDate(alert.triggeredAt) }} · Valeur {{ formatMetric(alert.observedValue) }} · Seuil {{ formatMetric(alert.threshold) }}
          </p>
        </div>
      </div>
      <div *ngIf="!compact && actionable" class="mt-3 flex gap-2 border-t border-line pt-2">
        <app-button size="xs" (onClick)="onSnooze.emit({ id: alert.id, minutes: 30 })">
          <app-svg-icon name="Clock" class="h-3.5 w-3.5"></app-svg-icon>
          Reporter 30 min
        </app-button>
        <app-button size="xs" variant="primary" (onClick)="onAcknowledge.emit(alert.id)">
          <app-svg-icon name="Check" class="h-3.5 w-3.5"></app-svg-icon>
          Accuser réception
        </app-button>
      </div>
    </article>
  `
})
export class AlertEventRowComponent {
  @Input() alert!: AlertEvent;
  @Input() compact = false;
  @Output() onAcknowledge = new EventEmitter<string>();
  @Output() onSnooze = new EventEmitter<{ id: string; minutes: number }>();

  get actionable(): boolean {
    return this.alert.status === 'active' || this.alert.status === 'snoozed';
  }

  get statusLabel(): string {
    const labels: Record<string, string> = {
      active: 'À traiter',
      acknowledged: 'Accusée',
      snoozed: 'Reportée',
      resolved: 'Résolue'
    };
    return labels[this.alert.status] ?? this.alert.status;
  }

  relativeDate(iso: string): string {
    return relativeDate(iso);
  }

  formatMetric(value: number): string {
    return formatMetric(value);
  }
}