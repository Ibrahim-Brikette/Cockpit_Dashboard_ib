-- V5: Add ON DELETE CASCADE to group FK references in share grant tables.
--
-- Before this migration, deleting a group row was blocked by PostgreSQL with a
-- FK constraint violation because dashboard_share_grant and query_share_grant
-- had no ON DELETE rule (defaulting to RESTRICT) on their grantee_group_id columns.
--
-- user_group_membership already had ON DELETE CASCADE from V1 and is unchanged.
--
-- Effect of this migration:
--   - Deleting a group automatically removes its dashboard and query share grants.
--   - User accounts are never touched — only the group-level access grants are removed.

ALTER TABLE cockpit.dashboard_share_grant
    DROP CONSTRAINT IF EXISTS fk_dashboard_share_group,
    ADD CONSTRAINT fk_dashboard_share_group
        FOREIGN KEY (grantee_group_id) REFERENCES cockpit.user_group (id)
        ON DELETE CASCADE;

ALTER TABLE cockpit.query_share_grant
    DROP CONSTRAINT IF EXISTS fk_query_share_group,
    ADD CONSTRAINT fk_query_share_group
        FOREIGN KEY (grantee_group_id) REFERENCES cockpit.user_group (id)
        ON DELETE CASCADE;
