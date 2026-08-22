-- ============================================================================
-- Multi-company migration: introduce app_company + user_company_access, and
-- backfill company_id onto the ledger/warehouse/transactional tables that now
-- require it.
-- ============================================================================
-- Run this ONCE, against a database that already has the tenant refactor
-- (migrate-to-multi-tenant.sql) and the warehouse-tenant retrofit
-- (migrate-warehouse-tenant.sql) applied. Take a database backup first - see
-- scripts/backup.sh.
--
-- This creates exactly ONE default Company per existing tenant and folds all
-- of that tenant's existing warehouses/chart-of-accounts/journals/purchases/
-- sales into it. If a tenant's data actually spans more than one real company,
-- split it manually after this runs (app_company + the FK columns below are
-- ordinary data at that point).
-- ============================================================================

BEGIN;

-- 1. app_company + user_company_access (brand new tables — see database-ddl.sql
--    for the canonical from-scratch definitions this mirrors).
CREATE SEQUENCE IF NOT EXISTS public.app_company_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE public.app_company (
    id bigint DEFAULT nextval('public.app_company_id_seq'::regclass) NOT NULL,
    tenant_id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    registration_no character varying(255),
    address character varying(255),
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone,
    created_by bigint,
    modified_at timestamp without time zone,
    modified_by bigint,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT app_company_pkey PRIMARY KEY (id),
    CONSTRAINT app_company_uk1 UNIQUE (tenant_id, code),
    CONSTRAINT app_company_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id)
);

CREATE SEQUENCE IF NOT EXISTS public.user_company_access_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE public.user_company_access (
    id bigint DEFAULT nextval('public.user_company_access_id_seq'::regclass) NOT NULL,
    tenant_id bigint NOT NULL,
    user_id bigint NOT NULL,
    company_id bigint NOT NULL,
    CONSTRAINT user_company_access_pkey PRIMARY KEY (id),
    CONSTRAINT user_company_access_uk1 UNIQUE (tenant_id, user_id, company_id),
    CONSTRAINT user_company_access_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id),
    CONSTRAINT user_company_access_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.user_profile(id),
    CONSTRAINT user_company_access_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.app_company(id)
);

-- 1b. warehouse_business_line (brand new table, no backfill target — every existing
--     warehouse gets VEHICLE_SALES by default below, since that's the only business
--     line that existed before this migration).
CREATE SEQUENCE IF NOT EXISTS public.warehouse_business_line_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE public.warehouse_business_line (
    id bigint DEFAULT nextval('public.warehouse_business_line_id_seq'::regclass) NOT NULL,
    tenant_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    business_line character varying(20) NOT NULL,
    CONSTRAINT warehouse_business_line_pkey PRIMARY KEY (id),
    CONSTRAINT warehouse_business_line_uk1 UNIQUE (tenant_id, warehouse_id, business_line),
    CONSTRAINT warehouse_business_line_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id),
    CONSTRAINT warehouse_business_line_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.inf_warehouse(id)
);

-- 2. One default Company per existing tenant.
INSERT INTO public.app_company (tenant_id, code, name, active, created_at, created_by)
SELECT id, 'MAIN', name, true, now(), 0
FROM public.fnd_tenant;

-- 3. Add company_id (nullable first) to every ledger/warehouse table.
ALTER TABLE public.inf_warehouse ADD COLUMN company_id bigint;
ALTER TABLE public.fnd_chart_of_accounts ADD COLUMN company_id bigint;
ALTER TABLE public.app_journal ADD COLUMN company_id bigint;
ALTER TABLE public.app_purchase_order ADD COLUMN company_id bigint;
ALTER TABLE public.app_purchase_order ADD COLUMN warehouse_id bigint;
ALTER TABLE public.app_sale ADD COLUMN company_id bigint;
ALTER TABLE public.user_profile ADD COLUMN default_company_id bigint;

-- 4. Backfill warehouse-adjacent tables first (inf_warehouse -> its tenant's
--    single default Company), everything else derives from this.
UPDATE public.inf_warehouse w
SET company_id = c.id
FROM public.app_company c
WHERE c.tenant_id = w.tenant_id AND c.code = 'MAIN';

-- 5. Backfill app_purchase_order.warehouse_id from its inventory line where one
--    was already assigned; otherwise fall back to the tenant's earliest
--    warehouse (the seeded 'MAIN' warehouse for tenants that never touched
--    multi-warehouse). Then company_id follows the resolved warehouse.
UPDATE public.app_purchase_order po
SET warehouse_id = COALESCE(
    (SELECT inv.warehouse_id
     FROM public.app_purchase_order_detail pod
     JOIN public.app_inventory inv ON inv.purchase_order_detail_id = pod.id
     WHERE pod.purchase_order_id = po.id AND inv.warehouse_id IS NOT NULL
     ORDER BY inv.id LIMIT 1),
    (SELECT w.id FROM public.inf_warehouse w WHERE w.tenant_id = po.tenant_id ORDER BY w.id LIMIT 1)
);

UPDATE public.app_purchase_order po
SET company_id = w.company_id
FROM public.inf_warehouse w
WHERE w.id = po.warehouse_id;

-- 6. Backfill app_sale.company_id the same way, via its inventory's warehouse,
--    falling back to the tenant's default warehouse's company for any sale
--    whose inventory predates the warehouse feature entirely.
UPDATE public.app_sale s
SET company_id = COALESCE(
    (SELECT w.company_id
     FROM public.app_inventory inv
     JOIN public.inf_warehouse w ON w.id = inv.warehouse_id
     WHERE inv.id = s.inventory_id),
    (SELECT c.id FROM public.app_company c WHERE c.tenant_id = s.tenant_id AND c.code = 'MAIN')
);

-- 7. Chart of accounts and journals aren't warehouse-linked — backfill
--    directly from the tenant's single default Company.
UPDATE public.fnd_chart_of_accounts coa
SET company_id = c.id
FROM public.app_company c
WHERE c.tenant_id = coa.tenant_id AND c.code = 'MAIN';

UPDATE public.app_journal j
SET company_id = c.id
FROM public.app_company c
WHERE c.tenant_id = j.tenant_id AND c.code = 'MAIN';

-- 8. Enforce NOT NULL + FKs now that every row is backfilled.
ALTER TABLE public.inf_warehouse ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE public.fnd_chart_of_accounts ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE public.app_journal ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE public.app_purchase_order ALTER COLUMN company_id SET NOT NULL;
ALTER TABLE public.app_purchase_order ALTER COLUMN warehouse_id SET NOT NULL;
ALTER TABLE public.app_sale ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE ONLY public.inf_warehouse ADD CONSTRAINT inf_warehouse_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.app_company(id);
ALTER TABLE ONLY public.fnd_chart_of_accounts ADD CONSTRAINT fnd_chart_of_accounts_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.app_company(id);
ALTER TABLE ONLY public.app_journal ADD CONSTRAINT app_journal_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.app_company(id);
ALTER TABLE ONLY public.app_purchase_order ADD CONSTRAINT app_purchase_order_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.app_company(id);
ALTER TABLE ONLY public.app_purchase_order ADD CONSTRAINT app_purchase_order_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.inf_warehouse(id);
ALTER TABLE ONLY public.app_sale ADD CONSTRAINT app_sale_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.app_company(id);
ALTER TABLE ONLY public.user_profile ADD CONSTRAINT user_profile_default_company_id_fkey FOREIGN KEY (default_company_id) REFERENCES public.app_company(id);

-- 8b. Every pre-existing warehouse only ever did vehicle sales — grant that business
--     line by default so nothing already in production loses the ability to receive
--     inventory/create purchases the moment this migration lands.
INSERT INTO public.warehouse_business_line (tenant_id, warehouse_id, business_line)
SELECT tenant_id, id, 'VEHICLE_SALES' FROM public.inf_warehouse;

-- 8c. RBAC resource for the new Company screens/endpoints (id chosen to continue
--     the existing fnd_resource sequence — adjust if this database's max id differs).
INSERT INTO public.fnd_resource (id, module_id, code, description, sort_order, active)
OVERRIDING SYSTEM VALUE
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM public.fnd_resource), 9, 'COMPANY', 'Companies', 15, true
WHERE NOT EXISTS (SELECT 1 FROM public.fnd_resource WHERE code = 'COMPANY');

-- 9. system_role uniqueness moves from per-tenant to per-tenant-per-company.
ALTER TABLE public.fnd_chart_of_accounts DROP CONSTRAINT IF EXISTS uq_coa_system_role;
DROP INDEX IF EXISTS public.uq_coa_system_role;
CREATE UNIQUE INDEX uq_coa_system_role ON public.fnd_chart_of_accounts USING btree (tenant_id, company_id, system_role) WHERE (system_role IS NOT NULL);

COMMIT;

-- Sanity checks after running (all should return 0):
--   SELECT count(*) FROM inf_warehouse WHERE company_id IS NULL;
--   SELECT count(*) FROM fnd_chart_of_accounts WHERE company_id IS NULL;
--   SELECT count(*) FROM app_journal WHERE company_id IS NULL;
--   SELECT count(*) FROM app_purchase_order WHERE company_id IS NULL OR warehouse_id IS NULL;
--   SELECT count(*) FROM app_sale WHERE company_id IS NULL;
--   SELECT count(*) FROM app_company GROUP BY tenant_id HAVING count(*) <> 1;  -- exactly 1 per tenant
