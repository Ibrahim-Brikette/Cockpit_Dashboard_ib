import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit, OnDestroy {
  form: FormGroup;
  loading      = false;
  showPassword = false;
  isDark       = false;
  toast: string | null = null;
  private toastTimer: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  ngOnInit(): void {
    this.isDark = document.documentElement.classList.contains('dark')
               || localStorage.getItem('theme') === 'dark';
    this.applyTheme();
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    localStorage.setItem('theme', this.isDark ? 'dark' : 'light');
    this.applyTheme();
  }

  private applyTheme(): void {
    document.documentElement.classList.toggle('dark', this.isDark);
  }

  get f() { return this.form.controls; }

  fieldError(name: string, error: string): boolean {
    const c = this.f[name];
    return !!(c.invalid && (c.dirty || c.touched) && c.errors?.[error]);
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.loading = true;
    const { username, password } = this.form.value;

    this.authService.login(username, password).pipe(
      finalize(() => { this.loading = false; this.cdr.markForCheck(); })
    ).subscribe({
      next:  () => this.router.navigate(['/']),
      error: (err: HttpErrorResponse) => this.showToast(this.mapError(err.status))
    });
  }

  showToast(msg: string): void {
    this.toast = msg;
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => { this.toast = null; this.cdr.markForCheck(); }, 5000);
  }

  dismissToast(): void {
    this.toast = null;
    clearTimeout(this.toastTimer);
  }

  private mapError(status: number): string {
    switch (status) {
      case 401: return 'Identifiant ou mot de passe incorrect.';
      case 423: return 'Compte verrouillé — consultez votre e-mail pour le réinitialiser.';
      case 429: return 'Trop de tentatives. Veuillez patienter avant de réessayer.';
      default:  return 'Une erreur est survenue. Veuillez réessayer.';
    }
  }

  ngOnDestroy(): void {
    clearTimeout(this.toastTimer);
  }
}
