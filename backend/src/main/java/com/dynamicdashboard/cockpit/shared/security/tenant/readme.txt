
  Hibernate filters are scoped to a single Session object. When you enable a filter on one Session, it has zero effect on any other Session.

  TenantContextFilter was doing this:

  HTTP Request arrives
    → filter runs (NO active transaction)
        → entityManager.unwrap(Session.class)  ← Spring creates a THROWAWAY Session (Session A)
        → sessionA.enableFilter("tenantFilter") ← filter enabled on Session A
        → sessionA.getEnabledFilter(...)        ← BUT the proxy may return a DIFFERENT Session A'
                                                   so it prints null
    → filter chain continues...
    → @Transactional service method runs
        → Spring opens a BRAND NEW Session (Session B) for the transaction
        → Session B has NO filters enabled ← your queries leak across tenants

  Every @Transactional method gets its own fresh Session. That Session knows nothing about what you did on a previous, unrelated Session.

  ---
  The Fix — 3 Moving Parts

  1. TenantContextFilter — only stores the tenant in a ThreadLocal

  // Before (wrong): tried to touch Hibernate here, outside any transaction
  Session session = entityManager.unwrap(Session.class);  // throwaway session
  session.enableFilter("tenantFilter")...                  // useless

  // After (correct): just remember who this request belongs to
  TenantContext.setCurrentTenant(UUID.fromString(tenantIdClaim));
  // Hibernate part is handled elsewhere, at the right moment

  The ThreadLocal is a safe place to park the tenant ID across the request thread. It gets cleared in finally so it never leaks to the next request.

  ---
  2. HibernateFilterConfiguration — controls the order transactions open

  @EnableTransactionManagement(order = Integer.MAX_VALUE - 100)

  Spring AOP works like nested try-blocks. Lower order number = outer block = runs first on entry, last on exit.

  By giving the transaction interceptor order MAX_VALUE - 100, it becomes the outermost advice around your service methods:

  ┌─── Transaction interceptor (order MAX_VALUE - 100) ─────────────────┐
  │  opens Session, starts transaction                                    │
  │  ┌─── TenantFilterAspect (order MAX_VALUE - 50) ──────────────────┐ │
  │  │  Session is ALREADY OPEN here → enableFilter() works correctly  │ │
  │  │  ┌─── actual @Transactional service method ──────────────────┐  │ │
  │  │  │  repository.findAll() → queries run WITH filter active     │  │ │
  │  │  └───────────────────────────────────────────────────────────┘  │ │
  │  └────────────────────────────────────────────────────────────────┘ │
  │  commits / closes Session                                            │
  └──────────────────────────────────────────────────────────────────────┘

  ---
  3. TenantFilterAspect — enables the filter on the real Session

  @Before("@annotation(Transactional) && within(com.dynamicdashboard.cockpit..*)")
  public void enableTenantFilter() {
      UUID tenantId = TenantContext.CURRENT_TENANT.get(); // read from ThreadLocal
      if (tenantId == null) return;

      Session session = entityManager.unwrap(Session.class); // NOW this is the real, transaction-bound Session
      session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
  }

  The @Before runs after the transaction opened (because of the ordering above) but before the method body executes. So when repository.findAll() runs, Hibernate automatically
  appends WHERE tenant_id = :tenantId to every SQL query on entities that have @Filter(name = "tenantFilter").

  ---
  Full Request Lifecycle (After Fix)

  1. HTTP request hits security filter chain
  2. JWT decoded → TenantContextFilter stores tenantId in ThreadLocal
  3. Request reaches controller → calls service method (@Transactional)
  4. Transaction interceptor (outer) → opens Hibernate Session B, starts transaction
  5. TenantFilterAspect (inner) → reads tenantId from ThreadLocal
                                 → calls Session B.enableFilter("tenantFilter")
                                 → Session B now has the filter ACTIVE
  6. Service method body runs → all repository queries on Session B
                              → Hibernate adds "AND tenant_id = ?" automatically
  7. Transaction interceptor → commits, closes Session B
  8. Finally block in TenantContextFilter → ThreadLocal.clear()


What is an Aspect?

  Normal code runs top to bottom. An Aspect is code that intercepts a method call without touching the method itself.

  Without Aspect:
    Controller → ServiceMethod() runs directly

  With Aspect:
    Controller → Spring intercepts → Aspect code runs first → ServiceMethod() runs

  You define where to intercept with a pointcut:
  @Before("@annotation(Transactional) && within(com.dynamicdashboard.cockpit..*)")
  "Before every @Transactional method in our package, run my code."

  No need to modify any service class — it applies automatically to all of them.

  ---
  @FilterDef vs @Filter — what each one does

  @FilterDef is a global declaration — registered once at startup in the SessionFactory:
  @FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
  "Hibernate, a filter named tenantFilter exists and it accepts a tenantId parameter."

  @Filter is a per-table opt-in — tells Hibernate what SQL to append on that specific table when the filter is active:
  @Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
  "When tenantFilter is enabled, add WHERE tenant_id = :tenantId to every query on this table."

  Without calling enableFilter() at runtime, @Filter does nothing — it's intentionally opt-in so you can bypass it for admin queries.

  ---
  Full Workflow — Step by Step

  ──────────────────────────────────────────────────
    APP STARTUP
  ──────────────────────────────────────────────────

    Hibernate scans entities:
      @FilterDef on DataQueryEntity
        → registers "tenantFilter" in the SessionFactory (global, once)

      @Filter on DataQueryEntity    → SQL rule for data_query table
      @Filter on DashboardEntity    → SQL rule for dashboard table
      @Filter on DataSourceEntity   → SQL rule for data_source table

    Rules are stored but INACTIVE — waiting to be switched on per Session.


  ──────────────────────────────────────────────────
    HTTP REQUEST ARRIVES
  ──────────────────────────────────────────────────

    1. JWT decoded by Spring Security

    2. TenantContextFilter runs
         reads "tenantId" claim from JWT → e.g. "abc-123"
         stores it in TenantContext (ThreadLocal)
         → just a variable on the current thread, nothing else

    3. Request reaches Controller → calls a Service method (@Transactional)

    4. Transaction interceptor (outer wrapper, order MAX_VALUE-100)
         opens a Hibernate Session   ← the real one, tied to a DB connection
         starts a transaction

    5. TenantFilterAspect intercepts (inner wrapper, order MAX_VALUE-50)
         reads tenantId from TenantContext ThreadLocal → "abc-123"
         calls:
           session.enableFilter("tenantFilter")
                  .setParameter("tenantId", "abc-123")
         → the Session now has the filter ACTIVE with the value set

    6. Service method body actually runs
         calls repository.findAll()
         Hibernate builds the SQL:

           SELECT * FROM data_query
           WHERE tenant_id = 'abc-123'   ← appended automatically by @Filter

           SELECT * FROM dashboard
           WHERE tenant_id = 'abc-123'   ← same

         Results returned only contain THIS tenant's data.

    7. Transaction interceptor commits, closes the Session
         → filter is gone with the Session (Sessions are not reused)

    8. TenantContextFilter finally block
         TenantContext.clear()  ← removes ThreadLocal to avoid leaking to next request


  ──────────────────────────────────────────────────
    NEXT REQUEST — clean slate, same process
  ──────────────────────────────────────────────────

  ---
  How the Classes "Communicate"

  TenantContextFilter
    │  writes tenantId into
    ▼
  TenantContext (ThreadLocal)
    │  read by
    ▼
  TenantFilterAspect
    │  calls enableFilter() on
    ▼
  Hibernate Session
    │  checks @Filter rules registered by
    ▼
  @FilterDef (DataQueryEntity) ──→ "tenantFilter" exists, needs tenantId param
  @Filter (DataQueryEntity)    ──→ append WHERE tenant_id = :tenantId on data_query
  @Filter (DashboardEntity)    ──→ append WHERE tenant_id = :tenantId on dashboard
  @Filter (DataSourceEntity)   ──→ append WHERE tenant_id = :tenantId on data_source
    │
    ▼
  SQL queries are automatically scoped to the current tenant

  The ThreadLocal is the bridge between the HTTP layer (filter) and the persistence layer (aspect). Each layer does one job and passes the information through it.

