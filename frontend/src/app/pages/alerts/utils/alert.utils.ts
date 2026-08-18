import { AlertComparisonOperator, AlertMetric } from '@core/models/types';

const METRIC_LABELS: Record<AlertMetric, string> = {
  total: 'Le total',
  average: 'La moyenne',
  maximum: 'Le maximum',
  minimum: 'Le minimum'
};

const OPERATOR_LABELS: Record<AlertComparisonOperator, string> = {
  gt: 'dépasse',
  lt: 'est inférieur à',
  eq: 'atteint'
};

export function metricLabel(metric: AlertMetric): string {
  return METRIC_LABELS[metric];
}

export function operatorLabel(operator: AlertComparisonOperator): string {
  return OPERATOR_LABELS[operator];
}

export function formatMetric(value: number): string {
  return new Intl.NumberFormat('fr-TN', { maximumFractionDigits: 1 }).format(value);
}