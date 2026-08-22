-- ============================================================================
-- Tenant provisioning template
-- ============================================================================
-- Backend-only, run-by-hand (psql -f, or paste into a psql session) - there is
-- deliberately no HTTP endpoint or UI for this, matching "tenant registration
-- stays backend-only" from the multi-tenancy design.
--
-- Replace every ALL-CAPS placeholder below, then run the whole file as one
-- transaction. Uses psql's \gset to carry the generated tenant/user ids between
-- statements, so run it with psql (not through a tool that only executes plain
-- SQL without meta-commands).
--
-- PASSWORD_BCRYPT_HASH: generate this with the app's own encoder - do not put a
-- plaintext password in this file or in shell history. From a Java/Groovy
-- console (or a throwaway JUnit test) in this project:
--     new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("the-real-password")
-- Any valid bcrypt hash works regardless of strength (the cost factor is
-- encoded in the hash itself); the app's SecurityConfig uses the default
-- BCryptPasswordEncoder() (strength 10).
--
-- This script provisions the tenant/roles/first user only. The Chart of
-- Accounts and default warehouse moved to provision-company-template.sql
-- (books are company-scoped, not tenant-scoped, since the multi-company
-- layer landed) — run that script immediately after this one, passing the
-- tenant_id it prints, before the tenant's SUPERADMIN logs in.
-- ============================================================================

BEGIN;

-- 1. Tenant -------------------------------------------------------------
INSERT INTO fnd_tenant (code, name, status, created_by, created_at, version)
VALUES ('TENANT_CODE', 'Tenant Display Name', 'ACTIVE', 0, now(), 0)
RETURNING id AS tenant_id \gset

-- 2. System roles ---------------------------------------------------------
-- Seeded empty of privileges. SUPERADMIN bypasses all privilege checks
-- entirely (see PrivilegeAspect) and needs no grants. ADMIN/STAFF grants are
-- configured afterwards by this tenant's own SUPERADMIN via the Roles screen
-- - per-tenant custom roles means each tenant's grants are independent, so
-- there is no "default" set to copy here.
INSERT INTO fnd_role (tenant_id, code, name, description, is_system, created_by, created_at, version) VALUES
  (:tenant_id, 'SUPERADMIN', 'Super Admin', 'Unrestricted access; bypasses privilege checks entirely', true, 0, now(), 0),
  (:tenant_id, 'ADMIN', 'Admin', 'Administrative access; privileges granted explicitly like any other role', true, 0, now(), 0),
  (:tenant_id, 'STAFF', 'Staff', 'Standard operational access', true, 0, now(), 0);

-- 3. First user (this tenant's own SUPERADMIN) -----------------------------
INSERT INTO user_profile (tenant_id, username, password, name, designation, deleted)
VALUES (:tenant_id, 'USERNAME', 'PASSWORD_BCRYPT_HASH', 'Full Name', 'Owner', false)
RETURNING id AS user_id \gset

INSERT INTO user_role (tenant_id, user_id, role_id)
SELECT :tenant_id, :user_id, id FROM fnd_role WHERE tenant_id = :tenant_id AND code = 'SUPERADMIN';

COMMIT;

-- Sanity check after running:
--   SELECT * FROM fnd_tenant WHERE id = :tenant_id;
--   SELECT * FROM fnd_role WHERE tenant_id = :tenant_id;
--   SELECT up.username, r.code FROM user_profile up
--     JOIN user_role ur ON ur.user_id = up.id JOIN fnd_role r ON r.id = ur.role_id
--     WHERE up.tenant_id = :tenant_id;
--
-- Next step (required): run provision-company-template.sql with :tenant_id
-- set to the id printed above — this tenant has no Chart of Accounts or
-- warehouse until that runs, so nothing can be posted or sold yet.
