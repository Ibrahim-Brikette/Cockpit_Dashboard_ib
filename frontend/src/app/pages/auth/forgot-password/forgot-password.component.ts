import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent implements OnInit, OnDestroy {
  form: FormGroup;
  loading = false;
  success = false;
  isDark  = false;
  toast: string | null = null;
  private toastTimer: any;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
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
    this.authService.forgotPassword(this.form.value.email).pipe(
      finalize(() => { this.loading = false; this.cdr.markForCheck(); })
    ).subscribe({
      // Always show success — backend never reveals whether the email is registered.
      next:  () => { this.success = true; this.cdr.markForCheck(); },
      error: () => { this.success = true; this.cdr.markForCheck(); }
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

  ngOnDestroy(): void {
    clearTimeout(this.toastTimer);
  }
}
