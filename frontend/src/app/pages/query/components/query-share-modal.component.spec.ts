import { ComponentFixture, TestBed, fakeAsync, tick, flush } from '@angular/core/testing';
import { SimpleChange }                                       from '@angular/core';
import { NEVER, of, throwError }                             from 'rxjs';

import { QueryShareModalComponent } from './query-share-modal.component';
import { SharingService }           from '@core/services/sharing.service';
import { IdentityService }          from '@core/services/identity.service';
import { GroupService }             from '@core/services/group.service';
import { AuditService }             from '@pages/settings/services/audit.service';
import { DataQuery }                from '@core/models/types';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

const MOCK_QUERY: DataQuery = {
  id:            'aaaaaaaa-0000-0000-0000-000000000001',
  name:          'Monthly Revenue',
  description:   '',
  visibility:    'shared',
  sourceIds:     [],
  joins:         [],
  selectedFieldIds: [],
  conditions:    [],
  groupByFieldIds:  [],
  aggregation:   'none',
  transformations:  [],
  rowLimit:      1000,
  usedByWidgets: 0,
  updatedAt:     new Date().toISOString()
} as DataQuery;

/** Opens the modal by simulating the parent binding open=true. */
function openModal(component: QueryShareModalComponent): void {
  component.query = MOCK_QUERY;
  component.open  = true;
  component.ngOnChanges({
    open:  new SimpleChange(false, true, false),
    query: new SimpleChange(null,  MOCK_QUERY, false)
  });
}

// ---------------------------------------------------------------------------
// Suite
// ---------------------------------------------------------------------------

describe('QueryShareModalComponent', () => {
  let fixture:         ComponentFixture<QueryShareModalComponent>;
  let component:       QueryShareModalComponent;
  let sharingService:  jasmine.SpyObj<SharingService>;
  let identityService: jasmine.SpyObj<IdentityService>;
  let groupService:    jasmine.SpyObj<GroupService>;
  let auditService:    jasmine.SpyObj<AuditService>;

  beforeEach(async () => {
    sharingService  = jasmine.createSpyObj('SharingService',  ['getQueryGrants', 'addQueryGrant', 'removeQueryGrant']);
    identityService = jasmine.createSpyObj('IdentityService', ['getAllUsers']);
    groupService    = jasmine.createSpyObj('GroupService',    ['getGroups']);
    auditService    = jasmine.createSpyObj('AuditService',    ['logAuditEvent']);

    await TestBed.configureTestingModule({
      imports: [QueryShareModalComponent],   // standalone component
      providers: [
        { provide: SharingService,  useValue: sharingService  },
        { provide: IdentityService, useValue: identityService },
        { provide: GroupService,    useValue: groupService    },
        { provide: AuditService,    useValue: auditService    }
      ]
    }).compileComponents();

    fixture   = TestBed.createComponent(QueryShareModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    // Destroy the component so ngOnDestroy clears timers and subscriptions.
    fixture.destroy();
  });

  // ── 1 ──────────────────────────────────────────────────────────────────────
  it('resolves to loaded-empty state when grants returns [] and all calls succeed',
    fakeAsync(() => {
      sharingService.getQueryGrants.and.returnValue(of([]));
      identityService.getAllUsers.and.returnValue(of([]));
      groupService.getGroups.and.returnValue(of([]));

      openModal(component);
      tick();          // flush microtasks / synchronous observables
      flush();         // flush any remaining timers (the 10 s safety net)
      fixture.detectChanges();

      expect(component.loading).toBeFalse();
      expect(component.error).toBe('');
      expect(component.accessList).toEqual([]);

      const body = fixture.nativeElement as HTMLElement;
      // Spinner must be gone
      expect(body.querySelector('.animate-spin')).toBeNull();
      // Empty-state message must be present
      expect(body.textContent).toContain('Aucun accès individuel configuré');
    })
  );

  // ── 2 ──────────────────────────────────────────────────────────────────────
  it('resolves to error state when a call never responds (server hangs)',
    fakeAsync(() => {
      // grants answers immediately; users and groups hang forever
      sharingService.getQueryGrants.and.returnValue(of([]));
      identityService.getAllUsers.and.returnValue(NEVER);
      groupService.getGroups.and.returnValue(NEVER);

      openModal(component);
      fixture.detectChanges();

      expect(component.loading).toBeTrue();   // still loading — as expected
      expect(component.error).toBe('');

      tick(7000);          // timeout(7000) fires on both NEVER observables
      fixture.detectChanges();

      expect(component.loading).toBeFalse();
      expect(component.error).toContain('serveur ne répond pas');

      flush();             // discard the 10 s safety-net timer
    })
  );

  // ── 3 ──────────────────────────────────────────────────────────────────────
  it('resolves to loaded-empty state when calls fail with HTTP errors (graceful degradation)',
    fakeAsync(() => {
      const httpError = { status: 403, statusText: 'Forbidden' };
      sharingService.getQueryGrants.and.returnValue(throwError(() => httpError));
      identityService.getAllUsers.and.returnValue(throwError(() => httpError));
      groupService.getGroups.and.returnValue(throwError(() => httpError));

      openModal(component);
      tick();
      flush();
      fixture.detectChanges();

      // HTTP errors are swallowed per-call → forkJoin completes with all []
      expect(component.loading).toBeFalse();
      expect(component.error).toBe('');
      expect(component.accessList).toEqual([]);
    })
  );

  // ── 4 ──────────────────────────────────────────────────────────────────────
  it('keeps "Enregistrer" disabled while data is loading',
    fakeAsync(() => {
      // All calls hang so loading stays true
      sharingService.getQueryGrants.and.returnValue(NEVER);
      identityService.getAllUsers.and.returnValue(NEVER);
      groupService.getGroups.and.returnValue(NEVER);

      openModal(component);
      fixture.detectChanges();

      const saveBtn = fixture.nativeElement.querySelector('button:last-of-type') as HTMLButtonElement;
      expect(component.loading).toBeTrue();
      expect(saveBtn.disabled).toBeTrue();

      flush();
    })
  );

  // ── 5 ──────────────────────────────────────────────────────────────────────
  it('keeps "Enregistrer" disabled when the error state is active',
    fakeAsync(() => {
      sharingService.getQueryGrants.and.returnValue(NEVER);
      identityService.getAllUsers.and.returnValue(NEVER);
      groupService.getGroups.and.returnValue(NEVER);

      openModal(component);
      tick(7000);           // trigger timeout → error state
      fixture.detectChanges();

      const saveBtn = fixture.nativeElement.querySelector('button:last-of-type') as HTMLButtonElement;
      expect(component.error).not.toBe('');
      expect(saveBtn.disabled).toBeTrue();   // [disabled]="saving || loading || !!error"

      flush();
    })
  );
});
