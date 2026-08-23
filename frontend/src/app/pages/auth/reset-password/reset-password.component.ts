import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '@core/services/auth.service';

function passwordsMatch(group: AbstractControl) {
  return group.get('newPassword')?.value === group.get('confirmPassword')?.value
    ? null : { mismatch: true };
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent implements OnInit, OnDestroy {
  form: FormGroup;
  loading         = false;
  success         = false;
  tokenInvalid    = false;
  showPassword    = false;
  showConfirm     = false;
  isDark          = false;
  toast: string | null = null;
  private token   = '';
  private toastTimer: any;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      newPassword:     ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: passwordsMatch });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
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

  get mismatchError(): boolean {
    const c = this.f['confirmPassword'];
    return !!(this.form.errors?.['mismatch'] && (c.dirty || c.touched));
  }

  get passwordStrength(): number {
    const pw: string = this.f['newPassword']?.value || '';
    if (!pw) return 0;
    let score = 0;
    if (pw.length >= 8)  score++;
    if (pw.length >= 12) score++;
    if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++;
    if (/[0-9]/.test(pw) && /[^A-Za-z0-9]/.test(pw)) score++;
    return Math.min(score, 4);
  }

  get strengthLabel(): string {
    return ['', 'Très faible', 'Faible', 'Moyen', 'Fort'][this.passwordStrength];
  }

  get strengthClass(): string {
    return ['', 'bg-negative', 'bg-caution', 'bg-caution', 'bg-positive'][this.passwordStrength];
  }

  get strengthWidth(): string {
    return ['0%', '25%', '50%', '75%', '100%'][this.passwordStrength];
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.loading = true;
    this.authService.resetPassword(this.token, this.form.value.newPassword).pipe(
      finalize(() => { this.loading = false; this.cdr.markForCheck(); })
    ).subscribe({
      next:  () => { this.success = true; this.cdr.markForCheck(); },
      error: (err: HttpErrorResponse) => {
        if (err.status === 400) {
          this.tokenInvalid = true;
          this.cdr.markForCheck();
        } else {
          this.showToast('Une erreur est survenue. Veuillez réessayer.');
        }
      }
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
