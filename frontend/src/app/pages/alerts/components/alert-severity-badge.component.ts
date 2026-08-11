import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';
import { AlertSeverity } from '@core/models/types';

@Component({
  selector: 'app-alert-severity-badge',
  standalone: true,
  imports: [CommonModule, SvgIconComponent],
  template: `
    <span class="inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-2xs font-semibold" [ngClass]="style">
      <app-svg-icon [name]="icon" class="h-3 w-3"></app-svg-icon>
      <ng-container *ngIf="!compact">{{ label }}</ng-container>
    </span>
  `
})
export class AlertSeverityBadgeComponent {
  @Input() severity: AlertSeverity = 'info';
  @Input() compact = false;

  private config: Record<AlertSeverity, { label: string; icon: string; style: string }> = {
    critical: { label: 'Critique', icon: 'ShieldAlert', style: 'bg-red-50 text-negative' },
    warning: { label: 'Avertissement', icon: 'AlertTriangle', style: 'bg-amber-50 text-caution' },
    info: { label: 'Information', icon: 'HelpCircle', style: 'bg-brand-soft text-brand-strong' }
  };

  get label(): string {
    return this.config[this.severity].label;
  }

  get icon(): string {
    return this.config[this.severity].icon;
  }

  get style(): string {
    return this.config[this.severity].style;
  }
}