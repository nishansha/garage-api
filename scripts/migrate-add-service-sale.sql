-- ============================================================================
-- Service-sale module: adds app_service / app_service_sale /
-- app_service_sale_item / app_service_sale_payment (all brand new tables, no
-- backfill needed) plus the SERVICE_OFFERING/SERVICE_SALE RBAC resources.
-- ============================================================================
-- Run this ONCE, after migrate-to-multi-company.sql has already been applied
-- (these tables reference app_company/inf_warehouse's company_id). Take a
-- database backup first - see scripts/backup.sh.
-- ============================================================================

BEGIN;

CREATE SEQUENCE IF NOT EXISTS public.sso_ref_no_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE SEQUENCE IF NOT EXISTS public.app_service_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE public.app_service (
    id bigint DEFAULT nextval('public.app_service_id_seq'::regclass) NOT NULL,
    tenant_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    default_rate numeric(15,2) NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone,
    created_by bigint,
    modified_at timestamp without time zone,
    modified_by bigint,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT app_service_pkey PRIMARY KEY (id),
    CONSTRAINT app_service_uk1 UNIQUE (tenant_id, warehouse_id, code),
    CONSTRAINT app_service_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id),
    CONSTRAINT app_service_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.inf_warehouse(id)
);

CREATE SEQUENCE IF NOT EXISTS public.app_service_sale_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE public.app_service_sale (
    id bigint DEFAULT nextval('public.app_service_sale_id_seq'::regclass) NOT NULL,
    tenant_id bigint NOT NULL,
    company_id bigint NOT NULL,
    warehouse_id bigint NOT NULL,
    invoice_no character varying(255) NOT NULL,
    customer_id bigint,
    walk_in_customer_name character varying(255),
    sale_date date NOT NULL,
    total_amount numeric(15,2) NOT NULL,
    payment_status character varying(20) NOT NULL,
    notes character varying(500),
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone,
    created_by bigint,
    modified_at timestamp without time zone,
    modified_by bigint,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT app_service_sale_pkey PRIMARY KEY (id),
    CONSTRAINT app_service_sale_uk1 UNIQUE (tenant_id, invoice_no),
    CONSTRAINT app_service_sale_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id),
    CONSTRAINT app_service_sale_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.app_company(id),
    CONSTRAINT app_service_sale_warehouse_id_fkey FOREIGN KEY (warehouse_id) REFERENCES public.inf_warehouse(id),
    CONSTRAINT app_service_sale_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.app_customer(id)
);

CREATE SEQUENCE IF NOT EXISTS public.app_service_sale_item_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE public.app_service_sale_item (
    id bigint DEFAULT nextval('public.app_service_sale_item_id_seq'::regclass) NOT NULL,
    tenant_id bigint NOT NULL,
    service_sale_id bigint NOT NULL,
    service_offering_id bigint,
    description character varying(255) NOT NULL,
    qty numeric(10,2) NOT NULL,
    rate numeric(15,2) NOT NULL,
    amount numeric(15,2) NOT NULL,
    created_at timestamp without time zone,
    created_by bigint,
    modified_at timestamp without time zone,
    modified_by bigint,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT app_service_sale_item_pkey PRIMARY KEY (id),
    CONSTRAINT app_service_sale_item_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id),
    CONSTRAINT app_service_sale_item_service_sale_id_fkey FOREIGN KEY (service_sale_id) REFERENCES public.app_service_sale(id),
    CONSTRAINT app_service_sale_item_service_offering_id_fkey FOREIGN KEY (service_offering_id) REFERENCES public.app_service(id)
);

CREATE SEQUENCE IF NOT EXISTS public.app_service_sale_payment_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE public.app_service_sale_payment (
    id bigint DEFAULT nextval('public.app_service_sale_payment_id_seq'::regclass) NOT NULL,
    tenant_id bigint NOT NULL,
    service_sale_id bigint NOT NULL,
    amount numeric(15,2) NOT NULL,
    payment_date date NOT NULL,
    payment_method character varying(20) NOT NULL,
    reference_no character varying(255),
    notes character varying(500),
    payment_account_id bigint NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone,
    created_by bigint,
    modified_at timestamp without time zone,
    modified_by bigint,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT app_service_sale_payment_pkey PRIMARY KEY (id),
    CONSTRAINT app_service_sale_payment_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id),
    CONSTRAINT app_service_sale_payment_service_sale_id_fkey FOREIGN KEY (service_sale_id) REFERENCES public.app_service_sale(id),
    CONSTRAINT app_service_sale_payment_payment_account_id_fkey FOREIGN KEY (payment_account_id) REFERENCES public.app_payment_account(id)
);

-- RBAC resources for the new screens/endpoints.
INSERT INTO public.fnd_resource (id, module_id, code, description, sort_order, active)
OVERRIDING SYSTEM VALUE
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM public.fnd_resource), 2, 'SERVICE_OFFERING', 'Services Catalog', 30, true
WHERE NOT EXISTS (SELECT 1 FROM public.fnd_resource WHERE code = 'SERVICE_OFFERING');

INSERT INTO public.fnd_resource (id, module_id, code, description, sort_order, active)
OVERRIDING SYSTEM VALUE
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM public.fnd_resource), 2, 'SERVICE_SALE', 'Service Sales', 40, true
WHERE NOT EXISTS (SELECT 1 FROM public.fnd_resource WHERE code = 'SERVICE_SALE');

-- SERVICE_REVENUE Chart-of-Account row for every existing company (new tenants get
-- this automatically from the updated provision-company-template.sql, but companies
-- provisioned before that update need it backfilled).
INSERT INTO public.fnd_chart_of_accounts
  (tenant_id, company_id, code, name, description, type, is_control_account, label, is_direct_postable, system_role, created_by, created_at, version)
SELECT c.tenant_id, c.id, '4010', 'R-SVC', 'Revenue from standalone services (e.g. car wash) not tied to a vehicle sale.', 'REVENUE', false, 'Service Revenue', false, 'SERVICE_REVENUE', 0, now(), 0
FROM public.app_company c
WHERE NOT EXISTS (
    SELECT 1 FROM public.fnd_chart_of_accounts coa
    WHERE coa.company_id = c.id AND coa.system_role = 'SERVICE_REVENUE'
);

COMMIT;

-- Sanity check after running:
--   SELECT * FROM fnd_resource WHERE code IN ('SERVICE_OFFERING', 'SERVICE_SALE');
--   SELECT company_id FROM fnd_chart_of_accounts WHERE system_role = 'SERVICE_REVENUE';
--   -- should list every row in app_company
