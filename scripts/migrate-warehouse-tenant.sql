-- ============================================================================
-- Warehouse tenant-scoping migration: backfill tenant_id + audit columns
-- onto inf_warehouse for a database that already ran migrate-to-multi-tenant.sql
-- ============================================================================
-- inf_warehouse was deliberately left out of the original multi-tenancy
-- migration (no Java entity/FK existed for it at the time). Run this ONCE,
-- after migrate-to-multi-tenant.sql has already been applied and fnd_tenant
-- is populated. Take a database backup first - see scripts/backup.sh.
--
-- If more than one tenant already exists in this database, replace the
-- subselect in step 2 below with the specific tenant id the existing
-- inf_warehouse row(s) actually belong to before running this.
-- ============================================================================

BEGIN;

-- 1. Add columns (nullable first)
ALTER TABLE public.inf_warehouse ADD COLUMN tenant_id bigint;
ALTER TABLE public.inf_warehouse ADD COLUMN created_at timestamp without time zone;
ALTER TABLE public.inf_warehouse ADD COLUMN created_by bigint;
ALTER TABLE public.inf_warehouse ADD COLUMN modified_at timestamp without time zone;
ALTER TABLE public.inf_warehouse ADD COLUMN modified_by bigint;
ALTER TABLE public.inf_warehouse ADD COLUMN version bigint DEFAULT 0 NOT NULL;

-- 2. Backfill existing rows onto the (single, pre-refactor) default tenant
UPDATE public.inf_warehouse
SET tenant_id = (SELECT id FROM public.fnd_tenant ORDER BY id LIMIT 1),
    created_at = now(),
    created_by = 0;

-- 3. Enforce NOT NULL + FK
ALTER TABLE public.inf_warehouse ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.inf_warehouse
    ADD CONSTRAINT inf_warehouse_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

-- 4. Fold tenant_id into the code uniqueness constraint
ALTER TABLE public.inf_warehouse DROP CONSTRAINT warehouse;
ALTER TABLE ONLY public.inf_warehouse ADD CONSTRAINT warehouse UNIQUE (tenant_id, code);

COMMIT;

-- Sanity check after running:
--   SELECT count(*) FROM inf_warehouse WHERE tenant_id IS NULL;  -- should be 0
