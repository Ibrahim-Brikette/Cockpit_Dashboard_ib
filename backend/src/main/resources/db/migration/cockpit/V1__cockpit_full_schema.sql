-- ============================================================
-- V1 — Full schema: core tables + auth + security
-- Combines what was previously V1 through V5.
-- ============================================================


-- ============================================================
-- USERS & ROLES
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.user_account (
    id                      UUID          PRIMARY KEY,
    version                 BIGINT        NOT NULL,
    created_at              TIMESTAMPTZ   NOT NULL,
    updated_at              TIMESTAMPTZ   NOT NULL,
    created_by              VARCHAR(100)  NOT NULL,
    updated_by              VARCHAR(100)  NOT NULL,
    username                VARCHAR(80)   NOT NULL,
    email                   VARCHAR(180)  NOT NULL,
    password_hash           VARCHAR(255)  NOT NULL,
    display_name            VARCHAR(120)  NOT NULL,
    account_status          VARCHAR(24)   NOT NULL,
    last_login_at           TIMESTAMPTZ,
    tenant_id               UUID          NOT NULL,
    -- brute-force protection (was V3 ALTER TABLE)
    failed_login_attempts   INTEGER       NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ   NULL,
    lockout_count           INTEGER       NOT NULL DEFAULT 0,
    is_permanently_locked   BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_user_account_username UNIQUE (username),
    CONSTRAINT uk_user_account_email    UNIQUE (email),
    CONSTRAINT ck_user_account_status   CHECK (account_status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING'))
);

CREATE TABLE IF NOT EXISTS cockpit.role (
    id                  UUID          PRIMARY KEY,
    version             BIGINT        NOT NULL,
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    created_by          VARCHAR(100)  NOT NULL,
    updated_by          VARCHAR(100)  NOT NULL,
    role_name           VARCHAR(80)   NOT NULL,
    role_description    VARCHAR(255),
    system_role         BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_role_name UNIQUE (role_name)
);

CREATE TABLE IF NOT EXISTS cockpit.permission (
    id                      UUID          PRIMARY KEY,
    version                 BIGINT        NOT NULL,
    created_at              TIMESTAMPTZ   NOT NULL,
    updated_at              TIMESTAMPTZ   NOT NULL,
    created_by              VARCHAR(100)  NOT NULL,
    updated_by              VARCHAR(100)  NOT NULL,
    permission_code         VARCHAR(120)  NOT NULL,
    permission_description  VARCHAR(255),
    CONSTRAINT uk_permission_code UNIQUE (permission_code)
);

CREATE TABLE IF NOT EXISTS cockpit.role_permission (
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role       FOREIGN KEY (role_id)       REFERENCES cockpit.role (id)       ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES cockpit.permission (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cockpit.user_role_assignment (
    id               UUID         PRIMARY KEY,
    version          BIGINT       NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(100) NOT NULL,
    updated_by       VARCHAR(100) NOT NULL,
    user_id          UUID         NOT NULL,
    role_id          UUID         NOT NULL,
    assignment_scope VARCHAR(24)  NOT NULL,
    CONSTRAINT fk_user_role_assignment_user FOREIGN KEY (user_id) REFERENCES cockpit.user_account (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_assignment_role FOREIGN KEY (role_id) REFERENCES cockpit.role (id)         ON DELETE CASCADE,
    CONSTRAINT uk_user_role_assignment       UNIQUE (user_id, role_id, assignment_scope),
    CONSTRAINT ck_user_role_scope            CHECK (assignment_scope IN ('GLOBAL', 'ORGANIZATION', 'GROUP'))
);

CREATE TABLE IF NOT EXISTS cockpit.user_group (
    id                UUID         PRIMARY KEY,
    version           BIGINT       NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    group_name        VARCHAR(120) NOT NULL,
    group_code        VARCHAR(80)  NOT NULL,
    group_description VARCHAR(255),
    CONSTRAINT uk_user_group_name UNIQUE (group_name),
    CONSTRAINT uk_user_group_code UNIQUE (group_code)
);

CREATE TABLE IF NOT EXISTS cockpit.user_group_membership (
    id              UUID         PRIMARY KEY,
    version         BIGINT       NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    group_id        UUID         NOT NULL,
    user_id         UUID         NOT NULL,
    membership_role VARCHAR(24)  NOT NULL,
    CONSTRAINT fk_user_group_membership_group FOREIGN KEY (group_id) REFERENCES cockpit.user_group (id)    ON DELETE CASCADE,
    CONSTRAINT fk_user_group_membership_user  FOREIGN KEY (user_id)  REFERENCES cockpit.user_account (id)  ON DELETE CASCADE,
    CONSTRAINT uk_user_group_membership        UNIQUE (group_id, user_id),
    CONSTRAINT ck_group_membership_role        CHECK (membership_role IN ('OWNER', 'ADMIN', 'MEMBER', 'READER'))
);


-- ============================================================
-- DATA SOURCES
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.db_connection (
    id              UUID         PRIMARY KEY,
    version         BIGINT       NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    updated_by      VARCHAR(100) NOT NULL,
    connection_name VARCHAR(120) NOT NULL,
    db_type         VARCHAR(40)  NOT NULL,
    db_host         VARCHAR(255) NOT NULL,
    db_port         INTEGER      NOT NULL,
    db_name         VARCHAR(120) NOT NULL,
    db_username     VARCHAR(120) NOT NULL,
    use_ssl         BOOLEAN      NOT NULL DEFAULT FALSE,
    vault_secret_key VARCHAR(255) NOT NULL,
    CONSTRAINT uk_db_connection_name UNIQUE (connection_name)
);

CREATE TABLE IF NOT EXISTS cockpit.data_source (
    id                  UUID         PRIMARY KEY,
    version             BIGINT       NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    source_key          VARCHAR(120) NOT NULL,
    source_label        VARCHAR(160) NOT NULL,
    source_description  VARCHAR(400),
    host_application    VARCHAR(40)  NOT NULL,
    db_connection_id    UUID,
    tenant_id           UUID         NOT NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_data_source_key              UNIQUE (source_key),
    CONSTRAINT fk_data_source_db_connection    FOREIGN KEY (db_connection_id) REFERENCES cockpit.db_connection (id)
);

CREATE TABLE IF NOT EXISTS cockpit.data_field (
    id                UUID         PRIMARY KEY,
    version           BIGINT       NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    created_by        VARCHAR(100) NOT NULL,
    updated_by        VARCHAR(100) NOT NULL,
    data_source_id    UUID         NOT NULL,
    field_key         VARCHAR(120) NOT NULL,
    field_label       VARCHAR(160) NOT NULL,
    field_type        VARCHAR(24)  NOT NULL,
    field_description VARCHAR(255),
    nullable          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_data_field_source    FOREIGN KEY (data_source_id) REFERENCES cockpit.data_source (id) ON DELETE CASCADE,
    CONSTRAINT uk_data_field_source_key UNIQUE (data_source_id, field_key),
    CONSTRAINT ck_data_field_type       CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'AMOUNT', 'PERCENT'))
);


-- ============================================================
-- QUERIES
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.data_query (
    id                   UUID         PRIMARY KEY,
    version              BIGINT       NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    created_by           VARCHAR(100) NOT NULL,
    updated_by           VARCHAR(100) NOT NULL,
    owner_id             UUID         NOT NULL,
    query_name           VARCHAR(160) NOT NULL,
    query_description    VARCHAR(500),
    visibility           VARCHAR(24)  NOT NULL,
    aggregation          VARCHAR(24)  NOT NULL,
    tenant_id            UUID         NOT NULL,
    aggregation_field_id UUID,
    row_limit            INTEGER      NOT NULL,
    used_by_widgets      INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT fk_data_query_owner             FOREIGN KEY (owner_id)             REFERENCES cockpit.user_account (id),
    CONSTRAINT fk_data_query_aggregation_field FOREIGN KEY (aggregation_field_id) REFERENCES cockpit.data_field (id),
    CONSTRAINT ck_query_visibility             CHECK (visibility   IN ('PERSONAL', 'SHARED')),
    CONSTRAINT ck_query_aggregation            CHECK (aggregation  IN ('NONE', 'SUM', 'AVG', 'MIN', 'MAX', 'COUNT')),
    CONSTRAINT ck_query_row_limit              CHECK (row_limit > 0 AND row_limit <= 100000)
);

CREATE TABLE IF NOT EXISTS cockpit.query_source_binding (
    id             UUID         PRIMARY KEY,
    version        BIGINT       NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    query_id       UUID         NOT NULL,
    data_source_id UUID         NOT NULL,
    position_index INTEGER      NOT NULL,
    CONSTRAINT fk_query_source_binding_query  FOREIGN KEY (query_id)       REFERENCES cockpit.data_query  (id) ON DELETE CASCADE,
    CONSTRAINT fk_query_source_binding_source FOREIGN KEY (data_source_id) REFERENCES cockpit.data_source (id),
    CONSTRAINT uk_query_source_binding         UNIQUE (query_id, data_source_id),
    CONSTRAINT ck_query_source_binding_position CHECK (position_index >= 0)
);

CREATE TABLE IF NOT EXISTS cockpit.query_selected_field (
    query_id       UUID    NOT NULL,
    field_id       UUID    NOT NULL,
    position_index INTEGER NOT NULL,
    PRIMARY KEY (query_id, field_id),
    CONSTRAINT fk_query_selected_field_query FOREIGN KEY (query_id) REFERENCES cockpit.data_query  (id) ON DELETE CASCADE,
    CONSTRAINT fk_query_selected_field_field FOREIGN KEY (field_id) REFERENCES cockpit.data_field  (id),
    CONSTRAINT ck_query_selected_field_position CHECK (position_index >= 0)
);

CREATE TABLE IF NOT EXISTS cockpit.query_group_by_field (
    query_id       UUID    NOT NULL,
    field_id       UUID    NOT NULL,
    position_index INTEGER NOT NULL,
    PRIMARY KEY (query_id, field_id),
    CONSTRAINT fk_query_group_by_field_query FOREIGN KEY (query_id) REFERENCES cockpit.data_query (id) ON DELETE CASCADE,
    CONSTRAINT fk_query_group_by_field_field FOREIGN KEY (field_id) REFERENCES cockpit.data_field (id),
    CONSTRAINT ck_query_group_by_field_position CHECK (position_index >= 0)
);

CREATE TABLE IF NOT EXISTS cockpit.query_join (
    id             UUID         PRIMARY KEY,
    version        BIGINT       NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    query_id       UUID         NOT NULL,
    left_source_id UUID         NOT NULL,
    left_field_id  UUID         NOT NULL,
    join_type      VARCHAR(24)  NOT NULL,
    right_source_id UUID        NOT NULL,
    right_field_id  UUID        NOT NULL,
    CONSTRAINT fk_query_join_query        FOREIGN KEY (query_id)        REFERENCES cockpit.data_query  (id) ON DELETE CASCADE,
    CONSTRAINT fk_query_join_left_source  FOREIGN KEY (left_source_id)  REFERENCES cockpit.data_source (id),
    CONSTRAINT fk_query_join_left_field   FOREIGN KEY (left_field_id)   REFERENCES cockpit.data_field  (id),
    CONSTRAINT fk_query_join_right_source FOREIGN KEY (right_source_id) REFERENCES cockpit.data_source (id),
    CONSTRAINT fk_query_join_right_field  FOREIGN KEY (right_field_id)  REFERENCES cockpit.data_field  (id),
    CONSTRAINT ck_query_join_type         CHECK (join_type IN ('INNER', 'LEFT', 'RIGHT', 'FULL'))
);

CREATE TABLE IF NOT EXISTS cockpit.query_condition (
    id               UUID         PRIMARY KEY,
    version          BIGINT       NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(100) NOT NULL,
    updated_by       VARCHAR(100) NOT NULL,
    query_id         UUID         NOT NULL,
    field_id         UUID         NOT NULL,
    operator         VARCHAR(24)  NOT NULL,
    value_expression VARCHAR(500) NOT NULL,
    logical_operator VARCHAR(4)   NOT NULL,
    parameterizable  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_query_condition_query FOREIGN KEY (query_id) REFERENCES cockpit.data_query (id) ON DELETE CASCADE,
    CONSTRAINT fk_query_condition_field FOREIGN KEY (field_id) REFERENCES cockpit.data_field  (id),
    CONSTRAINT ck_query_condition_operator CHECK (operator          IN ('EQ', 'NEQ', 'GT', 'LT', 'CONTAINS', 'IN', 'BETWEEN')),
    CONSTRAINT ck_query_condition_logical  CHECK (logical_operator  IN ('AND', 'OR'))
);

CREATE TABLE IF NOT EXISTS cockpit.query_transformation (
    id                  UUID         PRIMARY KEY,
    version             BIGINT       NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    created_by          VARCHAR(100) NOT NULL,
    updated_by          VARCHAR(100) NOT NULL,
    query_id            UUID         NOT NULL,
    transformation_type VARCHAR(24)  NOT NULL,
    target_field_id     UUID,
    output_label        VARCHAR(160),
    formula_expression  VARCHAR(800),
    format_option       VARCHAR(24),
    replacement_value   VARCHAR(255),
    operator            VARCHAR(24),
    comparison_value    VARCHAR(255),
    CONSTRAINT fk_query_transformation_query        FOREIGN KEY (query_id)        REFERENCES cockpit.data_query (id) ON DELETE CASCADE,
    CONSTRAINT fk_query_transformation_target_field FOREIGN KEY (target_field_id) REFERENCES cockpit.data_field (id),
    CONSTRAINT ck_query_transformation_type     CHECK (transformation_type IN ('RENAME', 'CALCULATED', 'FORMAT', 'REPLACE_EMPTY', 'FILTER_ROWS')),
    CONSTRAINT ck_query_transformation_operator CHECK (operator IS NULL OR operator IN ('EQ', 'NEQ', 'GT', 'LT', 'CONTAINS', 'IN', 'BETWEEN'))
);

CREATE TABLE IF NOT EXISTS cockpit.query_sort (
    query_id  UUID        PRIMARY KEY,
    field_id  UUID        NOT NULL,
    direction VARCHAR(8)  NOT NULL,
    CONSTRAINT fk_query_sort_query FOREIGN KEY (query_id) REFERENCES cockpit.data_query (id) ON DELETE CASCADE,
    CONSTRAINT fk_query_sort_field FOREIGN KEY (field_id) REFERENCES cockpit.data_field  (id),
    CONSTRAINT ck_query_sort_direction CHECK (direction IN ('ASC', 'DESC'))
);

CREATE TABLE IF NOT EXISTS cockpit.query_share_grant (
    id               UUID         PRIMARY KEY,
    version          BIGINT       NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(100) NOT NULL,
    updated_by       VARCHAR(100) NOT NULL,
    query_id         UUID         NOT NULL,
    share_level      VARCHAR(24)  NOT NULL,
    grantee_user_id  UUID,
    grantee_group_id UUID,
    access_level     VARCHAR(24)  NOT NULL,
    CONSTRAINT fk_query_share_query FOREIGN KEY (query_id)         REFERENCES cockpit.data_query  (id) ON DELETE CASCADE,
    CONSTRAINT fk_query_share_user  FOREIGN KEY (grantee_user_id)  REFERENCES cockpit.user_account (id),
    CONSTRAINT fk_query_share_group FOREIGN KEY (grantee_group_id) REFERENCES cockpit.user_group   (id),
    CONSTRAINT ck_query_share_level  CHECK (share_level  IN ('PRIVATE', 'USERS', 'GROUP', 'ORGANIZATION')),
    CONSTRAINT ck_query_share_access CHECK (access_level IN ('READ', 'EDIT', 'OWNER')),
    CONSTRAINT ck_query_share_target CHECK (
        (share_level = 'USERS'  AND grantee_user_id  IS NOT NULL AND grantee_group_id IS NULL) OR
        (share_level = 'GROUP'  AND grantee_group_id IS NOT NULL AND grantee_user_id  IS NULL) OR
        (share_level IN ('PRIVATE', 'ORGANIZATION') AND grantee_user_id IS NULL AND grantee_group_id IS NULL)
    )
);


-- ============================================================
-- DASHBOARDS & WIDGETS
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.dashboard (
    id                   UUID         PRIMARY KEY,
    version              BIGINT       NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    created_by           VARCHAR(100) NOT NULL,
    updated_by           VARCHAR(100) NOT NULL,
    owner_id             UUID         NOT NULL,
    dashboard_name       VARCHAR(180) NOT NULL,
    dashboard_description VARCHAR(500),
    color_hex            VARCHAR(12)  NOT NULL,
    status               VARCHAR(24)  NOT NULL,
    share_level          VARCHAR(24)  NOT NULL,
    columns_count        INTEGER      NOT NULL,
    density              VARCHAR(24)  NOT NULL,
    refresh_interval     VARCHAR(24)  NOT NULL,
    favorite             BOOLEAN      NOT NULL DEFAULT FALSE,
    archived             BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dashboard_owner        FOREIGN KEY (owner_id) REFERENCES cockpit.user_account (id),
    CONSTRAINT ck_dashboard_status       CHECK (status          IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT ck_dashboard_share_level  CHECK (share_level     IN ('PRIVATE', 'USERS', 'GROUP', 'ORGANIZATION')),
    CONSTRAINT ck_dashboard_columns      CHECK (columns_count BETWEEN 1 AND 24),
    CONSTRAINT ck_dashboard_density      CHECK (density         IN ('COMPACT', 'NORMAL', 'AIRY')),
    CONSTRAINT ck_dashboard_refresh      CHECK (refresh_interval IN ('OFF', 'S5', 'S10', 'S30', 'M1', 'M5', 'M15', 'M30', 'H1'))
);

CREATE TABLE IF NOT EXISTS cockpit.dashboard_tag (
    dashboard_id UUID        NOT NULL,
    tag_value    VARCHAR(80) NOT NULL,
    PRIMARY KEY (dashboard_id, tag_value),
    CONSTRAINT fk_dashboard_tag_dashboard FOREIGN KEY (dashboard_id) REFERENCES cockpit.dashboard (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cockpit.widget (
    id                       UUID         PRIMARY KEY,
    version                  BIGINT       NOT NULL,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    created_by               VARCHAR(100) NOT NULL,
    updated_by               VARCHAR(100) NOT NULL,
    dashboard_id             UUID         NOT NULL,
    widget_type              VARCHAR(24)  NOT NULL,
    widget_title             VARCHAR(180) NOT NULL,
    show_title               BOOLEAN      NOT NULL DEFAULT TRUE,
    widget_description       VARCHAR(500),
    query_id                 UUID,
    tenant_id                UUID         NOT NULL,
    grid_x                   INTEGER      NOT NULL,
    grid_y                   INTEGER      NOT NULL,
    grid_w                   INTEGER      NOT NULL,
    grid_h                   INTEGER      NOT NULL,
    refresh_interval         VARCHAR(24)  NOT NULL,
    navigate_to_dashboard_id UUID,
    kpi_format               VARCHAR(24),
    text_content             TEXT,
    CONSTRAINT fk_widget_dashboard     FOREIGN KEY (dashboard_id)             REFERENCES cockpit.dashboard  (id) ON DELETE CASCADE,
    CONSTRAINT fk_widget_query         FOREIGN KEY (query_id)                 REFERENCES cockpit.data_query (id),
    CONSTRAINT fk_widget_nav_dashboard FOREIGN KEY (navigate_to_dashboard_id) REFERENCES cockpit.dashboard  (id),
    CONSTRAINT ck_widget_type    CHECK (widget_type       IN ('KPI', 'BAR', 'STACKED', 'LINE', 'PIE', 'DONUT', 'HEATMAP', 'GAUGE', 'DATAGRID', 'TEXT')),
    CONSTRAINT ck_widget_refresh CHECK (refresh_interval  IN ('INHERIT', 'OFF', 'S5', 'S10', 'S30', 'M1', 'M5', 'M15', 'M30', 'H1')),
    CONSTRAINT ck_widget_kpi_format CHECK (kpi_format IS NULL OR kpi_format IN ('AMOUNT', 'PERCENT', 'INTEGER')),
    CONSTRAINT ck_widget_layout  CHECK (grid_w > 0 AND grid_h > 0)
);

CREATE TABLE IF NOT EXISTS cockpit.widget_datagrid_config (
    widget_id       UUID     PRIMARY KEY,
    rows_per_page   INTEGER,
    density         VARCHAR(24),
    show_toolbar    BOOLEAN,
    show_search     BOOLEAN,
    show_pagination BOOLEAN,
    show_totals     BOOLEAN,
    sortable        BOOLEAN,
    filterable      BOOLEAN,
    CONSTRAINT fk_widget_datagrid_widget FOREIGN KEY (widget_id) REFERENCES cockpit.widget (id) ON DELETE CASCADE,
    CONSTRAINT ck_widget_datagrid_density CHECK (density IS NULL OR density IN ('COMPACT', 'NORMAL', 'COMFORTABLE')),
    CONSTRAINT ck_widget_datagrid_rows    CHECK (rows_per_page IS NULL OR rows_per_page > 0)
);

CREATE TABLE IF NOT EXISTS cockpit.widget_datagrid_visible_column (
    widget_id      UUID         NOT NULL,
    column_name    VARCHAR(160) NOT NULL,
    position_index INTEGER      NOT NULL,
    PRIMARY KEY (widget_id, column_name),
    CONSTRAINT fk_widget_datagrid_visible_column FOREIGN KEY (widget_id) REFERENCES cockpit.widget_datagrid_config (widget_id) ON DELETE CASCADE,
    CONSTRAINT ck_widget_datagrid_visible_column_pos CHECK (position_index >= 0)
);

CREATE TABLE IF NOT EXISTS cockpit.widget_filter (
    id             UUID         PRIMARY KEY,
    version        BIGINT       NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    widget_id      UUID         NOT NULL,
    filter_label   VARCHAR(160),
    target_field_id UUID,
    operator       VARCHAR(24)  NOT NULL,
    filter_value   VARCHAR(500) NOT NULL,
    position_index INTEGER      NOT NULL,
    CONSTRAINT fk_widget_filter_widget       FOREIGN KEY (widget_id)       REFERENCES cockpit.widget      (id) ON DELETE CASCADE,
    CONSTRAINT fk_widget_filter_target_field FOREIGN KEY (target_field_id) REFERENCES cockpit.data_field  (id),
    CONSTRAINT ck_widget_filter_operator     CHECK (operator IN ('EQ', 'NEQ', 'GT', 'LT', 'CONTAINS', 'IN', 'BETWEEN'))
);

CREATE TABLE IF NOT EXISTS cockpit.global_filter (
    id             UUID         PRIMARY KEY,
    version        BIGINT       NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    created_by     VARCHAR(100) NOT NULL,
    updated_by     VARCHAR(100) NOT NULL,
    dashboard_id   UUID         NOT NULL,
    filter_name    VARCHAR(120) NOT NULL,
    filter_label   VARCHAR(160) NOT NULL,
    input_type     VARCHAR(24)  NOT NULL,
    target_field_id UUID,
    default_value  VARCHAR(255) NOT NULL,
    reader_visible BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_global_filter_dashboard FOREIGN KEY (dashboard_id)   REFERENCES cockpit.dashboard  (id) ON DELETE CASCADE,
    CONSTRAINT fk_global_filter_field     FOREIGN KEY (target_field_id) REFERENCES cockpit.data_field (id),
    CONSTRAINT ck_global_filter_input     CHECK (input_type IN ('SELECT', 'DATE', 'DATERANGE', 'TEXT', 'MULTISELECT'))
);

CREATE TABLE IF NOT EXISTS cockpit.global_filter_option (
    filter_id      UUID         NOT NULL,
    option_value   VARCHAR(160) NOT NULL,
    position_index INTEGER      NOT NULL,
    PRIMARY KEY (filter_id, option_value),
    CONSTRAINT fk_global_filter_option_filter FOREIGN KEY (filter_id) REFERENCES cockpit.global_filter (id) ON DELETE CASCADE,
    CONSTRAINT ck_global_filter_option_pos    CHECK (position_index >= 0)
);

CREATE TABLE IF NOT EXISTS cockpit.global_filter_value_map (
    filter_id UUID         NOT NULL,
    map_key   VARCHAR(160) NOT NULL,
    map_value VARCHAR(500) NOT NULL,
    PRIMARY KEY (filter_id, map_key),
    CONSTRAINT fk_global_filter_value_map_filter FOREIGN KEY (filter_id) REFERENCES cockpit.global_filter (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cockpit.dashboard_share_grant (
    id               UUID         PRIMARY KEY,
    version          BIGINT       NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(100) NOT NULL,
    updated_by       VARCHAR(100) NOT NULL,
    dashboard_id     UUID         NOT NULL,
    share_level      VARCHAR(24)  NOT NULL,
    grantee_user_id  UUID,
    grantee_group_id UUID,
    access_level     VARCHAR(24)  NOT NULL,
    CONSTRAINT fk_dashboard_share_dashboard FOREIGN KEY (dashboard_id)     REFERENCES cockpit.dashboard   (id) ON DELETE CASCADE,
    CONSTRAINT fk_dashboard_share_user      FOREIGN KEY (grantee_user_id)  REFERENCES cockpit.user_account (id),
    CONSTRAINT fk_dashboard_share_group     FOREIGN KEY (grantee_group_id) REFERENCES cockpit.user_group   (id),
    CONSTRAINT ck_dashboard_share_level  CHECK (share_level  IN ('PRIVATE', 'USERS', 'GROUP', 'ORGANIZATION')),
    CONSTRAINT ck_dashboard_share_access CHECK (access_level IN ('READ', 'EDIT', 'OWNER')),
    CONSTRAINT ck_dashboard_share_target CHECK (
        (share_level = 'USERS'  AND grantee_user_id  IS NOT NULL AND grantee_group_id IS NULL) OR
        (share_level = 'GROUP'  AND grantee_group_id IS NOT NULL AND grantee_user_id  IS NULL) OR
        (share_level IN ('PRIVATE', 'ORGANIZATION') AND grantee_user_id IS NULL AND grantee_group_id IS NULL)
    )
);


-- ============================================================
-- AUDIT
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.audit_event (
    id           UUID         PRIMARY KEY,
    version      BIGINT       NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_by   VARCHAR(100) NOT NULL,
    actor_user_id UUID,
    event_type   VARCHAR(120) NOT NULL,
    target_type  VARCHAR(80)  NOT NULL,
    target_id    UUID,
    details_json TEXT,
    source_ip    VARCHAR(64),
    user_agent   VARCHAR(255),
    occurred_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_audit_event_actor FOREIGN KEY (actor_user_id) REFERENCES cockpit.user_account (id)
);


-- ============================================================
-- AUTH — refresh tokens & session tracking
-- token_hash = SHA-256 hex digest; raw token never stored.
-- last_used_at  → idle-timeout check (30 min)
-- original_login_at → absolute-timeout check (8h)
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.refresh_token (
    id                UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    token_hash        CHAR(64)    NOT NULL,
    session_id        UUID        NOT NULL,
    user_id           UUID        NOT NULL REFERENCES cockpit.user_account (id) ON DELETE CASCADE,
    expires_at        TIMESTAMPTZ NOT NULL,
    original_login_at TIMESTAMPTZ NOT NULL,
    last_used_at      TIMESTAMPTZ NOT NULL,
    revoked           BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);


-- ============================================================
-- AUTH — password reset tokens
-- Shared by brute-force lockout reset and forgot-password flow.
-- expires_at = now() + 5 min. used flips to true after consumption.
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.password_reset_token (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    token_hash CHAR(64)    NOT NULL,
    user_id    UUID        NOT NULL REFERENCES cockpit.user_account (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash)
);


-- ============================================================
-- AUTH — email verification tokens
-- Created on user account creation (STANDALONE mode).
-- expires_at = now() + 24h. used flips to true after consumption.
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.email_verification_token (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    token_hash CHAR(64)    NOT NULL,
    user_id    UUID        NOT NULL REFERENCES cockpit.user_account (id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_email_verification_token_hash UNIQUE (token_hash)
);


-- ============================================================
-- AUTH — JTI blacklist (logout / token revocation)
-- One row per revoked JWT. Cleaned up when expires_at passes.
-- jti is the UUID stored in the JWT "jti" claim.
-- ============================================================

CREATE TABLE IF NOT EXISTS cockpit.jti_blacklist (
    jti        UUID        NOT NULL PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES cockpit.user_account (id) ON DELETE CASCADE,
    revoked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_data_query_owner           ON cockpit.data_query           (owner_id);
CREATE INDEX IF NOT EXISTS idx_dashboard_owner            ON cockpit.dashboard             (owner_id);
CREATE INDEX IF NOT EXISTS idx_widget_dashboard           ON cockpit.widget                (dashboard_id);
CREATE INDEX IF NOT EXISTS idx_widget_filter_widget       ON cockpit.widget_filter         (widget_id);
CREATE INDEX IF NOT EXISTS idx_query_condition_query      ON cockpit.query_condition       (query_id);
CREATE INDEX IF NOT EXISTS idx_query_join_query           ON cockpit.query_join            (query_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_occurred_at    ON cockpit.audit_event           (occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_dashboard_share_dashboard  ON cockpit.dashboard_share_grant (dashboard_id);
CREATE INDEX IF NOT EXISTS idx_query_share_query          ON cockpit.query_share_grant     (query_id);
CREATE INDEX IF NOT EXISTS idx_data_source_db_connection  ON cockpit.data_source           (db_connection_id);

CREATE INDEX IF NOT EXISTS idx_refresh_token_session_id   ON cockpit.refresh_token (session_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id      ON cockpit.refresh_token (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_active_user  ON cockpit.refresh_token (user_id, original_login_at) WHERE revoked = FALSE;

CREATE INDEX IF NOT EXISTS idx_password_reset_token_user_id ON cockpit.password_reset_token (user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_token_active  ON cockpit.password_reset_token (user_id, expires_at) WHERE used = FALSE;

CREATE INDEX IF NOT EXISTS idx_email_verification_token_user_id ON cockpit.email_verification_token (user_id);

CREATE INDEX IF NOT EXISTS idx_jti_blacklist_expires_at ON cockpit.jti_blacklist (expires_at);
