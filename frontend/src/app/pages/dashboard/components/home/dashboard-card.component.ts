import { DashboardService } from '@pages/dashboard/services/dashboard.service';
import { PermissionService } from '@core/services/permission.service';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Dashboard } from '@core/models/types';
import { relativeDate } from '@core/services/utils';
import { BadgeComponent } from '@shared/components/ui/badge.component';
import { ShareModalComponent } from '@pages/dashboard/components/home/share-modal.component';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';
import { ConfirmModalComponent } from '@shared/components/ui/confirm-modal.component';
import { DashboardAccessPopoverComponent } from '@pages/dashboard/components/home/dashboard-access-popover.component';

@Component({
  selector: 'app-dashboard-card',
  standalone: true,
  imports: [CommonModule, BadgeComponent, ShareModalComponent, SvgIconComponent, ConfirmModalComponent, DashboardAccessPopoverComponent],
  template: `
    <div
      (click)="onOpen.emit()"
      class="group relative cursor-pointer rounded-lg border border-line bg-white shadow-card transition-shadow hover:shadow-pop"
      [class.z-30]="menuOpen"
    >
      <div class="h-1 rounded-t-lg" [style.backgroundColor]="dashboard.color"></div>
      <div class="p-3">
        <div class="mb-1.5 flex items-start justify-between gap-2">
          <div class="flex min-w-0 items-center gap-1.5">
            <h3 class="truncate text-sm font-semibold text-ink">
              {{ dashboard.name }}
            </h3>
            <app-badge *ngIf="dashboard.status === 'draft'" tone="caution">
              Brouillon
            </app-badge>
          </div>
          <div class="flex flex-shrink-0 items-center gap-0.5" (click)="$event.stopPropagation()">
            <button
              (click)="toggleFav()"
              [attr.aria-label]="dashboard.favorite ? 'Retirer des favoris' : 'Ajouter aux favoris'"
              class="rounded p-1 text-ink-faint hover:bg-surface-sunken"
            >
              <app-svg-icon
                name="Star"
                [class]="'h-3.5 w-3.5 ' + (dashboard.favorite ? 'fill-caution text-caution' : '')"
              ></app-svg-icon>
            </button>
            <div class="relative">
              <button
                (click)="menuOpen = !menuOpen"
                class="rounded p-1 text-ink-faint hover:bg-surface-sunken"
              >
                ⋮
              </button>
              <div *ngIf="menuOpen" class="fixed inset-0 z-20" (click)="menuOpen = false"></div>
              <div
                *ngIf="menuOpen"
                class="absolute right-0 z-30 mt-1 w-44 rounded-md border border-line bg-white py-1 shadow-pop"
              >
                <button
                  *ngIf="permissionService.canManageDashboard(dashboard)"
                  (click)="navigate('/editeur/' + dashboard.id)"
                  class="flex w-full items-center gap-2 px-2.5 py-1.5 text-left text-xs text-ink-soft hover:bg-surface-muted"
                >
                  <app-svg-icon name="Pencil" class="h-3.5 w-3.5"></app-svg-icon>
                  Modifier
                </button>
                <button
                  *ngIf="permissionService.canManageDashboard(dashboard) && permissionService.canCreateDashboard()"
                  (click)="duplicate()"
                  class="flex w-full items-center gap-2 px-2.5 py-1.5 text-left text-xs text-ink-soft hover:bg-surface-muted"
                >
                  <app-svg-icon name="Copy" class="h-3.5 w-3.5"></app-svg-icon>
                  Dupliquer
                </button>
                <button
                  *ngIf="permissionService.canShareDashboard(dashboard)"
                  (click)="shareOpen = true; menuOpen = false"
                  class="flex w-full items-center gap-2 px-2.5 py-1.5 text-left text-xs text-ink-soft hover:bg-surface-muted"
                >
                  <app-svg-icon name="Share2" class="h-3.5 w-3.5"></app-svg-icon>
                  Partager
                </button>
                <button
                  *ngIf="permissionService.canManageDashboard(dashboard)"
                  (click)="archive()"
                  class="flex w-full items-center gap-2 px-2.5 py-1.5 text-left text-xs text-ink-soft hover:bg-surface-muted"
                >
                  <app-svg-icon name="Archive" class="h-3.5 w-3.5"></app-svg-icon>
                  {{ dashboard.archived ? 'Désarchiver' : 'Archiver' }}
                </button>
                <div *ngIf="permissionService.canDeleteDashboard(dashboard)" class="my-1 border-t border-line"></div>
                <button
                  *ngIf="permissionService.canDeleteDashboard(dashboard)"
                  (click)="deleteDash()"
                  class="flex w-full items-center gap-2 px-2.5 py-1.5 text-left text-xs text-negative hover:bg-surface-muted"
                >
                  <app-svg-icon name="Trash2" class="h-3.5 w-3.5"></app-svg-icon>
                  Supprimer
                </button>
              </div>
            </div>
          </div>
        </div>
        <p class="mb-3 line-clamp-2 h-8 text-xs text-ink-faint">
          {{ dashboard.description || 'Aucune description.' }}
        </p>
        <div class="flex flex-wrap items-center gap-2.5 text-2xs text-ink-faint">
          <span class="inline-flex items-center gap-1">
            <app-svg-icon name="Layers" class="h-3.5 w-3.5"></app-svg-icon>
            {{ dashboard.widgets.length }} widgets
          </span>
          <div class="relative" (click)="$event.stopPropagation()">
            <app-dashboard-access-popover
              [dashboard]="dashboard"
              [canShare]="permissionService.canShareDashboard(dashboard)"
              (onManage)="shareOpen = true"
            ></app-dashboard-access-popover>
          </div>
          <span class="ml-auto">{{ getRelDate(dashboard.updatedAt, dashboard.createdAt) }}</span>
        </div>
      </div>
    </div>
    <app-share-modal
      [dashboard]="dashboard"
      [open]="shareOpen"
      (onClose)="shareOpen = false"
    ></app-share-modal>
    <app-confirm-modal
      [open]="deleteConfirmOpen"
      title="Supprimer le tableau de bord"
      [message]="'Êtes-vous sûr de vouloir supprimer le tableau de bord « ' + dashboard.name + ' » ? Cette action est irréversible.'"
      (onClose)="deleteConfirmOpen = false"
      (onConfirm)="confirmDeleteDash()"
    ></app-confirm-modal>
  `
})
export class DashboardCardComponent {
  @Input() dashboard!: Dashboard;
  @Output() onOpen = new EventEmitter<void>();
  menuOpen: boolean = false;
  shareOpen: boolean = false;
  deleteConfirmOpen: boolean = false;
  constructor(
    private dashboardService: DashboardService,
    private router: Router,
    public permissionService: PermissionService
  ) {}
  getRelDate(iso: string, createdAt?: string): string {
    return relativeDate(iso, createdAt);
  }
  toggleFav() {
    this.dashboardService.toggleFavorite(this.dashboard.id);
  }
  duplicate() {
    this.dashboardService.duplicateDashboard(this.dashboard.id);
    this.menuOpen = false;
  }
  archive() {
    this.dashboardService.archiveDashboard(this.dashboard.id);
    this.menuOpen = false;
  }
  deleteDash() {
    this.menuOpen = false;
    this.deleteConfirmOpen = true;
  }
  confirmDeleteDash() {
    this.dashboardService.deleteDashboard(this.dashboard.id);
    this.deleteConfirmOpen = false;
  }
  navigate(path: string) {
    this.menuOpen = false;
    this.router.navigateByUrl(path);
  }
}
