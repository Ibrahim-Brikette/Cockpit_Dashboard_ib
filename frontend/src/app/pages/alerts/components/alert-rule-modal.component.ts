import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ModalComponent } from '@shared/components/ui/modal.component';
import { ButtonComponent } from '@shared/components/ui/button.component';
import { AlertChannel, AlertChannelConfig, AlertRule, DataQuery } from '@core/models/types';
import { uid } from '@core/utils/utils';

const CHANNEL_LABELS: Record<AlertChannel, string> = {
  inApp: 'Dans CockpitNG',
  email: 'E-mail',
  sms: 'SMS',
  whatsapp: 'WhatsApp'
};

function blankRule(): AlertRule {
  const now = new Date().toISOString();
  return {
    id: uid('rule'),
    name: 'Nouvelle règle d’alerte',
    queryId: '',
    metric: 'total',
    operator: 'gt',
    threshold: 0,
    severity: 'warning',
    enabled: true,
    channels: [
      { channel: 'inApp', enabled: true },
      { channel: 'email', enabled: false, recipient: '' },
      { channel: 'sms', enabled: false, recipient: '' },
      { channel: 'whatsapp', enabled: false, recipient: '' }
    ],
    createdAt: now,
    updatedAt: now
  };
}

@Component({
  selector: 'app-alert-rule-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent, ButtonComponent],
  templateUrl: './alert-rule-modal.component.html'
})
export class AlertRuleModalComponent implements OnChanges {
  @Input() open = false;
  @Input() rule?: AlertRule;
  @Input() queries: DataQuery[] = [];
  @Output() onClose = new EventEmitter<void>();
  @Output() onSave = new EventEmitter<AlertRule>();

  draft: AlertRule = blankRule();
  channelLabels = CHANNEL_LABELS;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) {
      this.draft = this.rule
        ? { ...this.rule, channels: this.rule.channels.map((channel) => ({ ...channel })) }
        : blankRule();
    }
  }

  get title(): string {
    return this.rule ? 'Modifier la règle d’alerte' : 'Nouvelle règle d’alerte';
  }

  get canSave(): boolean {
    return !!this.draft.name.trim() && !!this.draft.queryId;
  }

  patchChannel(channel: AlertChannel, patch: Partial<AlertChannelConfig>): void {
    this.draft = {
      ...this.draft,
      channels: this.draft.channels.map((item) => (item.channel === channel ? { ...item, ...patch } : item))
    };
  }

  channelRecipientPlaceholder(channel: AlertChannel): string {
    return channel === 'email' ? 'finance@entreprise.tn' : '+216 XX XXX XXX';
  }

  close(): void {
    this.onClose.emit();
  }

  save(): void {
    if (!this.canSave) return;
    this.onSave.emit({ ...this.draft, name: this.draft.name.trim() });
    this.close();
  }
}