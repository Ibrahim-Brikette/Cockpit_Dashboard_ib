import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, combineLatest } from 'rxjs';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';
import { BadgeComponent } from '@shared/components/ui/badge.component';
import { DashboardService } from '@pages/dashboard/services/dashboard.service';
import { QueryService } from '@pages/query/services/query.service';
import { AnalyticsService } from '@pages/admin-kpi/services/analytics.service';
import { AnalyticsEvent, Dashboard, DataQuery } from '@core/models/types';
import { formatNumber, formatPercent, relativeDate } from '@core/utils/utils';
import {
  RankedMetric,
  actionLabel,
  activitySeries,
  engagementRate,
  topDashboardMetrics,
  topQueryMetrics
} from '@pages/admin-kpi/utils/analytics.utils';

type ActivityFilter = 'all' | AnalyticsEvent['action'];

interface WidgetViewsRow {
  id: string;
  label: string;
  dashboard: string;
  views: number;
}

@Component({
  selector: 'app-admin-kpi',
  standalone: true,
  imports: [CommonModule, SvgIconComponent, BadgeComponent],
  templateUrl: './admin-kpi.component.html'
})
export class AdminKpiComponent implements OnInit, OnDestroy {
  readonly filters: { value: ActivityFilter; label: string }[] = [
    { value: 'all', label: 'Toutes' },
    { value: 'dashboard_view', label: 'Tableaux' },
    { value: 'raw_data_view', label: 'Données brutes' },
    { value: 'query_execution', label: 'Requêtes' }
  ];

  filter: ActivityFilter = 'all';

  dashboards: Dashboard[] = [];
  queries: DataQuery[] = [];
  dataSourcesCount = 0;

  events: AnalyticsEvent[] = [];
  seriesPoints: { label: string; dashboardViews: number; rawDataViews: number; activeUsers: number }[] = [];

  dashboardViews = 0;
  rawDataViews = 0;
  queryRuns = 0;
  activeUsers = 0;
  impressions = 0;
  clicksEngagement = 0;
  usersCount = 0;
  totalWidgets = 0;

  topDashboards: (RankedMetric & { detail: string })[] = [];
  topQueries: (RankedMetric & { detail: string })[] = [];
  topWidgets: WidgetViewsRow[] = [];

  chartAreaPath = '';
  chartLinePath = '';
  chartRawDataLinePath = '';
  chartGridLines: { y: number; value: number }[] = [];
  hoveredIndex: number | null = null;
  /** Position verticale (en %) du curseur dans la zone du graphe, pour que le tooltip suive la souris. */
  hoveredYPercent = 50;

  private subscription?: Subscription;

  constructor(
    private dashboardService: DashboardService,
    private queryService: QueryService,
    private analyticsService: AnalyticsService
  ) {}

  ngOnInit(): void {
    this.dashboardService.loadFromBackend();
    this.queryService.loadFromBackend();
    this.analyticsService.loadFromBackend();

    this.subscription = combineLatest([
      this.dashboardService.dashboards$,
      this.queryService.queries$,
      this.queryService.catalogSources$,
      this.analyticsService.overview$
    ]).subscribe(([dashboards, queries, catalogSources, overview]) => {
      this.dashboards = dashboards;
      this.queries = queries;
      this.dataSourcesCount = catalogSources?.length ?? 0;

      const counters = overview.counters;
      this.dashboardViews = Object.values(counters.dashboardViews).reduce((sum, value) => sum + value, 0);
      this.rawDataViews = Object.values(counters.widgetRawDataViews).reduce((sum, value) => sum + value, 0);
      this.queryRuns = Object.values(counters.queryExecutions).reduce((sum, value) => sum + value, 0);
      this.impressions = counters.impressions;
      this.clicksEngagement = engagementRate(counters);

      // On ne garde que les jours réellement actifs (comme côté React :
      // un jour où l'application n'a pas été ouverte n'a jamais d'entrée),
      // puis on limite aux 7 derniers jours actifs.
      const daily = overview.daily;
      const activeDaily = daily.filter(
        (point) => point.activeUsers > 0 || point.dashboardViews > 0 || point.rawDataViews > 0
      );
      const last7 = activeDaily.slice(-7);
      this.activeUsers = last7[last7.length - 1]?.activeUsers ?? 0;
      this.seriesPoints = activitySeries(last7);

      this.topDashboards = topDashboardMetrics(dashboards, counters).map((item) => ({ ...item, detail: 'consultations' }));
      this.topQueries = topQueryMetrics(queries, counters).map((item) => ({ ...item, detail: 'exécutions' }));

      const widgets = dashboards.flatMap((dashboard) =>
        dashboard.widgets.map((widget) => ({ ...widget, dashboardName: dashboard.name }))
      );
      this.topWidgets = Object.entries(counters.widgetRawDataViews)
        .map(([id, views]) => {
          const widget = widgets.find((item) => item.id === id);
          return {
            id,
            label: widget?.title ?? 'Widget supprimé',
            dashboard: widget?.dashboardName ?? '—',
            views
          };
        })
        .sort((a, b) => b.views - a.views)
        .slice(0, 5);

      this.events = overview.events;
      this.usersCount = new Set(overview.events.map((event) => event.userId)).size;
      this.totalWidgets = dashboards.reduce((sum, dashboard) => sum + dashboard.widgets.length, 0);

      this.buildChartPaths();
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  get activities(): AnalyticsEvent[] {
    return this.events.filter((event) => this.filter === 'all' || event.action === this.filter).slice(0, 8);
  }

  get avgViewsPerDashboard(): number {
    return this.dashboards.length ? Math.round(this.dashboardViews / this.dashboards.length) : 0;
  }

  setFilter(value: ActivityFilter): void {
    this.filter = value;
  }

  formatNumber(v: number): string {
    return formatNumber(v);
  }

  formatPercent(v: number): string {
    return formatPercent(v);
  }

  relativeDate(iso: string): string {
    return relativeDate(iso);
  }

  actionLabel(action: AnalyticsEvent['action']): string {
    return actionLabel(action);
  }

  eventTone(event: AnalyticsEvent): 'brand' | 'positive' | 'caution' {
    if (event.action === 'raw_data_view' || event.action === 'raw_data_export') return 'caution';
    if (event.action === 'query_execution') return 'positive';
    return 'brand';
  }

  rankedBarWidth(value: number, max: number): number {
    if (!max) return 8;
    return Math.max(8, (value / max) * 100);
  }

  /**
   * Calcule un pas d'axe "rond", comme le fait recharts côté React (ex: 0,30,60,90,120
   * pour un pic à ~92), afin que la courbe remplisse la hauteur du graphique au lieu de
   * laisser un grand vide au-dessus. On arrondit le pas au chiffre entier supérieur
   * (1 à 10) à sa magnitude, au lieu de le forcer sur la grille grossière {1,2,5,10} qui
   * pouvait doubler inutilement le maximum (ex: 92 -> pas 50 -> max 200 au lieu de
   * pas 30 -> max 120).
   */
  private get chartAxisStep(): number {
    const raw = Math.max(1, ...this.seriesPoints.map((p) => Math.max(p.dashboardViews, p.rawDataViews)));
    const roughStep = raw / 4;
    const magnitude = Math.pow(10, Math.floor(Math.log10(roughStep || 1)));
    const normalized = roughStep / magnitude;
    const niceNormalized = Math.min(10, Math.max(1, Math.ceil(normalized)));
    return niceNormalized * magnitude;
  }

  private get chartMax(): number {
    return this.chartAxisStep * 4;
  }

  private valueToSvgY(value: number, max: number): number {
    return 100 - (value / max) * 92 - 4;
  }

  private buildChartPaths(): void {
    const points = this.seriesPoints;
    if (!points.length) {
      this.chartAreaPath = '';
      this.chartLinePath = '';
      this.chartRawDataLinePath = '';
      this.chartGridLines = [];
      return;
    }
    const max = this.chartMax;
    const stepX = points.length > 1 ? 300 / (points.length - 1) : 300;
    const toCoords = (key: 'dashboardViews' | 'rawDataViews') =>
      points.map((point, index) => ({
        x: index * stepX,
        y: this.valueToSvgY(point[key], max)
      }));

    /**
     * Interpolation cubique monotone (même algorithme que d3/recharts "type=monotone",
     * conversion Hermite → Bézier), pour que la courbe suive fidèlement les vraies
     * valeurs sans créer de "vagues" artificielles comme le faisait l'ancien tracé
     * en courbes de Bézier quadratiques enchaînées.
     */
    const buildSmoothPath = (coords: { x: number; y: number }[]) => {
      const n = coords.length;
      if (!n) return '';
      if (n === 1) return `M${coords[0].x.toFixed(2)},${coords[0].y.toFixed(2)}`;
      if (n === 2) {
        return `M${coords[0].x.toFixed(2)},${coords[0].y.toFixed(2)} L${coords[1].x.toFixed(2)},${coords[1].y.toFixed(2)}`;
      }

      // 1. Pentes entre points consécutifs.
      const dx: number[] = [];
      const slope: number[] = [];
      for (let i = 0; i < n - 1; i++) {
        dx.push(coords[i + 1].x - coords[i].x);
        slope.push((coords[i + 1].y - coords[i].y) / dx[i]);
      }

      // 2. Tangentes initiales (moyenne des pentes adjacentes).
      const tangent: number[] = new Array(n);
      tangent[0] = slope[0];
      tangent[n - 1] = slope[n - 2];
      for (let i = 1; i < n - 1; i++) {
        tangent[i] = (slope[i - 1] + slope[i]) / 2;
      }

      // 3. Contraintes de Fritsch-Carlson pour garantir la monotonie
      //    (empêche tout dépassement/rebond artificiel entre les points).
      for (let i = 0; i < n - 1; i++) {
        if (slope[i] === 0) {
          tangent[i] = 0;
          tangent[i + 1] = 0;
          continue;
        }
        const a = tangent[i] / slope[i];
        const b = tangent[i + 1] / slope[i];
        const h = Math.sqrt(a * a + b * b);
        if (h > 3) {
          const t = 3 / h;
          tangent[i] = t * a * slope[i];
          tangent[i + 1] = t * b * slope[i];
        }
      }

      // 4. Conversion Hermite -> Bézier cubique (commandes SVG "C").
      let path = `M${coords[0].x.toFixed(2)},${coords[0].y.toFixed(2)}`;
      for (let i = 0; i < n - 1; i++) {
        const p0 = coords[i];
        const p1 = coords[i + 1];
        const d = dx[i] / 3;
        const c1x = p0.x + d;
        const c1y = p0.y + tangent[i] * d;
        const c2x = p1.x - d;
        const c2y = p1.y - tangent[i + 1] * d;
        path += ` C${c1x.toFixed(2)},${c1y.toFixed(2)} ${c2x.toFixed(2)},${c2y.toFixed(2)} ${p1.x.toFixed(2)},${p1.y.toFixed(2)}`;
      }
      return path;
    };

    const viewsCoords = toCoords('dashboardViews');
    this.chartLinePath = buildSmoothPath(viewsCoords);
    this.chartAreaPath = `${this.chartLinePath} L${(points.length - 1) * stepX},100 L0,100 Z`;
    this.chartRawDataLinePath = buildSmoothPath(toCoords('rawDataViews'));

    // i=0 correspond à la ligne du haut (valeur max) et i=steps à la ligne du bas (0),
    // pour que la liste des libellés (rendue de haut en bas dans le template) affiche
    // bien "120, 90, 60, 30, 0" et non l'inverse.
    const steps = 4;
    const step = this.chartAxisStep;
    this.chartGridLines = Array.from({ length: steps + 1 }, (_, i) => {
      const value = step * (steps - i);
      return { y: this.valueToSvgY(value, max), value };
    });
  }

  onChartMove(event: MouseEvent, host: HTMLElement): void {
    if (!this.seriesPoints.length) return;
    const rect = host.getBoundingClientRect();
    const ratioX = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width));
    this.hoveredIndex = Math.round(ratioX * (this.seriesPoints.length - 1));

    // Le tooltip suit verticalement la position réelle de la souris dans la zone
    // du graphe (comme le fait le Tooltip de recharts côté React), au lieu de
    // rester figé en haut du graphique.
    const ratioY = Math.min(1, Math.max(0, (event.clientY - rect.top) / rect.height));
    this.hoveredYPercent = Math.min(88, Math.max(4, ratioY * 100));
  }

  onChartLeave(): void {
    this.hoveredIndex = null;
  }

  /** Survol d'une barre dans le widget "Données brutes consultées" (tooltip au hover, comme React). */
  hoveredWidgetIndex: number | null = null;
  tooltipWidgetX = 0;
  tooltipWidgetY = 0;

  /**
   * Le tooltip suit la souris horizontalement, et se place au-dessus de la ligne
   * survolée quand c'est la dernière (pour ne pas sortir de la carte), sinon
   * en-dessous — comme le comportement d'un tooltip recharts qui reste toujours
   * visible dans son conteneur.
   */
 onWidgetBarMove(event: MouseEvent, index: number, wrapper: HTMLElement): void {
  this.hoveredWidgetIndex = index;

  const wrapperRect = wrapper.getBoundingClientRect();
  const rowRect = (event.currentTarget as HTMLElement).getBoundingClientRect();

  const tooltipWidth = 160;
  const tooltipHeight = 54;

  let x = event.clientX - wrapperRect.left + 12;
  x = Math.min(x, wrapperRect.width - tooltipWidth - 4);
  x = Math.max(x, 4);
  this.tooltipWidgetX = x;

  const rowBottomInWrapper = rowRect.bottom - wrapperRect.top;
  const wouldOverflowBelow = rowBottomInWrapper + tooltipHeight + 6 > wrapperRect.height;

  const rowTopInWrapper = rowRect.top - wrapperRect.top;
  this.tooltipWidgetY = wouldOverflowBelow
    ? rowTopInWrapper - tooltipHeight - 6
    : rowBottomInWrapper + 6;
}

  onWidgetBarLeave(): void {
    this.hoveredWidgetIndex = null;
  }

  get hoveredPoint(): { label: string; dashboardViews: number; rawDataViews: number; activeUsers: number } | null {
    return this.hoveredIndex !== null ? this.seriesPoints[this.hoveredIndex] : null;
  }

  get hoveredXPercent(): number {
    if (this.hoveredIndex === null || this.seriesPoints.length < 2) return 0;
    return (this.hoveredIndex / (this.seriesPoints.length - 1)) * 100;
  }

  get tooltipLeftPercent(): number {
    return Math.min(98, Math.max(2, this.hoveredXPercent));
  }

  /**
   * Le tooltip se place à côté du trait de la courbe (comme recharts côté React),
   * pas centré dessus : à droite du point par défaut, et il bascule à gauche quand
   * on approche du bord droit du graphique pour ne jamais sortir du cadre.
   */
  get tooltipSide(): 'left' | 'right' {
    return this.hoveredXPercent > 62 ? 'left' : 'right';
  }

  get hoveredXSvg(): number {
    if (this.hoveredIndex === null || this.seriesPoints.length < 2) return 0;
    return (this.hoveredIndex / (this.seriesPoints.length - 1)) * 300;
  }

  get hoveredDashboardYSvg(): number {
    if (!this.hoveredPoint) return 0;
    return this.valueToSvgY(this.hoveredPoint.dashboardViews, this.chartMax);
  }

  get hoveredRawDataYSvg(): number {
    if (!this.hoveredPoint) return 0;
    return this.valueToSvgY(this.hoveredPoint.rawDataViews, this.chartMax);
  }
}