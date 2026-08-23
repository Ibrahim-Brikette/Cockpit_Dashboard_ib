import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { DbConnectionService, DbConnectionResponse } from './services/db-connection.service';
import { PermissionService } from '@core/services/permission.service';
import { Router, RouterModule } from '@angular/router';
import { SvgIconComponent } from '@shared/components/svg-icon/svg-icon.component';
import { ButtonComponent } from '@shared/components/ui/button.component';
import { BadgeComponent } from '@shared/components/ui/badge.component';
import { DataSourceModalComponent } from './components/data-source-modal.component';
import { ConfirmModalComponent } from '@shared/components/ui/confirm-modal.component';
export interface CatalogDataSource {
  id: string;
  key: string;
  label: string;
  description: string;
  app: string;
  active: boolean;
  fields?: any[];
}
@Component({
  selector: 'app-data-sources',
  standalone: true,
  imports: [CommonModule, RouterModule, SvgIconComponent, ButtonComponent, BadgeComponent, DataSourceModalComponent, ConfirmModalComponent],
  templateUrl: './data-sources.component.html'
})
export class DataSourcesComponent implements OnInit {
  databases: DbConnectionResponse[] = [];
  catalogSources: CatalogDataSource[] = [];
  loading = false;
  error = '';
  showEditor = false;
  selectedSource: any = null;
  showDeleteModal = false;
  dbToDelete: DbConnectionResponse | null = null;
  constructor(
    private router: Router,
    private dbConnectionService: DbConnectionService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    public permissionService: PermissionService
  ) {}
  ngOnInit() {
    this.fetchConnections();
  }
  fetchConnections() {
    this.dbConnectionService.getAllConnections().subscribe({
      next: (data) => {
        this.databases = data || [];
        this.cdr.detectChanges();
        this.fetchCatalogSources();
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des connexions.';
        this.cdr.detectChanges();
        this.fetchCatalogSources();
      }
    });
  }
  fetchCatalogSources() {
    this.http.get<CatalogDataSource[]>('/api/catalog/data-sources').subscribe({
      next: (sources) => {
        this.catalogSources = sources || [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.catalogSources = [];
        this.cdr.detectChanges();
      }
    });
  }
  promptDelete(db: DbConnectionResponse) {
    this.dbToDelete = db;
    this.showDeleteModal = true;
  }
  confirmDelete() {
    if (!this.dbToDelete) return;
    this.dbConnectionService.deleteConnection(this.dbToDelete.id).subscribe({
      next: () => {
        this.showDeleteModal = false;
        this.dbToDelete = null;
        this.fetchConnections();
      },
      error: (err) => {
        alert('Erreur lors de la suppression de la connexion.');
      }
    });
  }
  openEditor(source?: any) {
    this.selectedSource = source || null;
    this.showEditor = true;
  }
  onModalClose() {
    this.showEditor = false;
    this.fetchConnections();
  }
  goToQueries() {
    this.router.navigate(['/requetes']);
  }
}
