-- ============================================================================
-- Multi-tenancy migration: backfill tenant_id onto an EXISTING database
-- ============================================================================
-- Run this ONCE against a database that predates the multi-tenancy refactor
-- (i.e. has real data but no tenant_id columns yet). Do NOT re-run
-- scripts/database-ddl.sql against a live database - that file is a
-- from-scratch schema reference, not an incremental migration.
--
-- Replace DEFAULT_TENANT_CODE / DEFAULT_TENANT_NAME below, then run the whole
-- file inside a single transaction (psql -f, or paste into one psql session).
-- Take a database backup first - see scripts/backup.sh.
-- ============================================================================

BEGIN;

-- 1. fnd_tenant table + the single default tenant that inherits all existing data
CREATE TABLE public.fnd_tenant (
    id bigint NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    status character varying(20) NOT NULL,
    created_by bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    modified_by bigint,
    modified_at timestamp(6) without time zone,
    version bigint DEFAULT 0 NOT NULL
);

ALTER TABLE public.fnd_tenant ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.fnd_tenant_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE ONLY public.fnd_tenant ADD CONSTRAINT fnd_tenant_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.fnd_tenant ADD CONSTRAINT fnd_tenant_code_key UNIQUE (code);

INSERT INTO public.fnd_tenant (code, name, status, created_by, created_at, version)
VALUES ('DEFAULT_TENANT_CODE', 'DEFAULT_TENANT_NAME', 'ACTIVE', 0, now(), 0)
RETURNING id AS default_tenant_id \gset

-- 2. Add tenant_id to every tenant-owned table: nullable -> backfill -> NOT NULL -> FK
ALTER TABLE public.app_customer ADD COLUMN tenant_id bigint;
UPDATE public.app_customer SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_customer ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_customer ADD CONSTRAINT app_customer_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_direct_entry ADD COLUMN tenant_id bigint;
UPDATE public.app_direct_entry SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_direct_entry ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_direct_entry ADD CONSTRAINT app_direct_entry_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_expense ADD COLUMN tenant_id bigint;
UPDATE public.app_expense SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_expense ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_expense ADD CONSTRAINT app_expense_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_inventory ADD COLUMN tenant_id bigint;
UPDATE public.app_inventory SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_inventory ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_inventory ADD CONSTRAINT app_inventory_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_journal ADD COLUMN tenant_id bigint;
UPDATE public.app_journal SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_journal ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_journal ADD CONSTRAINT app_journal_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_journal_detail ADD COLUMN tenant_id bigint;
UPDATE public.app_journal_detail SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_journal_detail ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_journal_detail ADD CONSTRAINT app_journal_detail_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_payment_account ADD COLUMN tenant_id bigint;
UPDATE public.app_payment_account SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_payment_account ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_payment_account ADD CONSTRAINT app_payment_account_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_product ADD COLUMN tenant_id bigint;
UPDATE public.app_product SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_product ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_product ADD CONSTRAINT app_product_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_purchase_order ADD COLUMN tenant_id bigint;
UPDATE public.app_purchase_order SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_purchase_order ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_purchase_order ADD CONSTRAINT app_purchase_order_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_purchase_order_detail ADD COLUMN tenant_id bigint;
UPDATE public.app_purchase_order_detail SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_purchase_order_detail ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_purchase_order_detail ADD CONSTRAINT app_purchase_order_detail_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_purchase_payment ADD COLUMN tenant_id bigint;
UPDATE public.app_purchase_payment SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_purchase_payment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_purchase_payment ADD CONSTRAINT app_purchase_payment_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_purchase_return ADD COLUMN tenant_id bigint;
UPDATE public.app_purchase_return SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_purchase_return ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_purchase_return ADD CONSTRAINT app_purchase_return_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_purchase_return_receipt ADD COLUMN tenant_id bigint;
UPDATE public.app_purchase_return_receipt SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_purchase_return_receipt ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_purchase_return_receipt ADD CONSTRAINT app_purchase_return_receipt_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_sale ADD COLUMN tenant_id bigint;
UPDATE public.app_sale SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_sale ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_sale ADD CONSTRAINT app_sale_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_sale_amount_split ADD COLUMN tenant_id bigint;
UPDATE public.app_sale_amount_split SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_sale_amount_split ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_sale_amount_split ADD CONSTRAINT app_sale_amount_split_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_sale_payment ADD COLUMN tenant_id bigint;
UPDATE public.app_sale_payment SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_sale_payment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_sale_payment ADD CONSTRAINT app_sale_payment_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_sale_refund_payment ADD COLUMN tenant_id bigint;
UPDATE public.app_sale_refund_payment SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_sale_refund_payment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_sale_refund_payment ADD CONSTRAINT app_sale_refund_payment_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_sale_return ADD COLUMN tenant_id bigint;
UPDATE public.app_sale_return SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_sale_return ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_sale_return ADD CONSTRAINT app_sale_return_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_sale_return_deduction ADD COLUMN tenant_id bigint;
UPDATE public.app_sale_return_deduction SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_sale_return_deduction ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_sale_return_deduction ADD CONSTRAINT app_sale_return_deduction_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_transaction ADD COLUMN tenant_id bigint;
UPDATE public.app_transaction SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_transaction ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_transaction ADD CONSTRAINT app_transaction_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.app_vendor ADD COLUMN tenant_id bigint;
UPDATE public.app_vendor SET tenant_id = :default_tenant_id;
ALTER TABLE public.app_vendor ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.app_vendor ADD CONSTRAINT app_vendor_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.fnd_chart_of_accounts ADD COLUMN tenant_id bigint;
UPDATE public.fnd_chart_of_accounts SET tenant_id = :default_tenant_id;
ALTER TABLE public.fnd_chart_of_accounts ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.fnd_chart_of_accounts ADD CONSTRAINT fnd_chart_of_accounts_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.fnd_role ADD COLUMN tenant_id bigint;
UPDATE public.fnd_role SET tenant_id = :default_tenant_id;
ALTER TABLE public.fnd_role ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.fnd_role ADD CONSTRAINT fnd_role_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.fnd_role_privilege ADD COLUMN tenant_id bigint;
UPDATE public.fnd_role_privilege SET tenant_id = :default_tenant_id;
ALTER TABLE public.fnd_role_privilege ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.fnd_role_privilege ADD CONSTRAINT fnd_role_privilege_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.user_profile ADD COLUMN tenant_id bigint;
UPDATE public.user_profile SET tenant_id = :default_tenant_id;
ALTER TABLE public.user_profile ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.user_profile ADD CONSTRAINT user_profile_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.user_refresh_token ADD COLUMN tenant_id bigint;
UPDATE public.user_refresh_token SET tenant_id = :default_tenant_id;
ALTER TABLE public.user_refresh_token ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.user_refresh_token ADD CONSTRAINT user_refresh_token_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.user_role ADD COLUMN tenant_id bigint;
UPDATE public.user_role SET tenant_id = :default_tenant_id;
ALTER TABLE public.user_role ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.user_role ADD CONSTRAINT user_role_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

ALTER TABLE public.user_session ADD COLUMN tenant_id bigint;
UPDATE public.user_session SET tenant_id = :default_tenant_id;
ALTER TABLE public.user_session ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ONLY public.user_session ADD CONSTRAINT user_session_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id);

-- 3. revinfo gets a plain nullable tenant_id (existing rows predate multi-tenancy, stay NULL)
ALTER TABLE public.revinfo ADD COLUMN tenant_id bigint;

-- 4. Fold tenant_id into composite unique constraints that need it
ALTER TABLE public.fnd_role DROP CONSTRAINT fnd_role_code_key;
ALTER TABLE ONLY public.fnd_role ADD CONSTRAINT fnd_role_code_key UNIQUE (tenant_id, code);

ALTER TABLE public.fnd_role_privilege DROP CONSTRAINT fnd_role_privilege_role_id_resource_id_privilege_key;
ALTER TABLE ONLY public.fnd_role_privilege ADD CONSTRAINT fnd_role_privilege_role_id_resource_id_privilege_key UNIQUE (tenant_id, role_id, resource_id, privilege);

ALTER TABLE public.app_vendor DROP CONSTRAINT mobile_uk;
ALTER TABLE ONLY public.app_vendor ADD CONSTRAINT mobile_uk UNIQUE (tenant_id, mobile);

ALTER TABLE public.app_product DROP CONSTRAINT product_uk1;
ALTER TABLE ONLY public.app_product ADD CONSTRAINT product_uk1 UNIQUE (tenant_id, sku);

ALTER TABLE public.app_purchase_order_detail DROP CONSTRAINT purchase_detail_uk1;
ALTER TABLE ONLY public.app_purchase_order_detail ADD CONSTRAINT purchase_detail_uk1 UNIQUE (tenant_id, purchase_order_id, product_id);
ALTER TABLE public.app_purchase_order_detail DROP CONSTRAINT purchase_detail_uk2;
ALTER TABLE ONLY public.app_purchase_order_detail ADD CONSTRAINT purchase_detail_uk2 UNIQUE (tenant_id, product_id, item_uid);

ALTER TABLE public.app_sale DROP CONSTRAINT uk8f8cd9ubu8iqrbtngbquvfvgv;
ALTER TABLE ONLY public.app_sale ADD CONSTRAINT uk8f8cd9ubu8iqrbtngbquvfvgv UNIQUE (tenant_id, invoice_no);

ALTER TABLE public.app_inventory DROP CONSTRAINT vehicle_uk;
ALTER TABLE ONLY public.app_inventory ADD CONSTRAINT vehicle_uk UNIQUE (tenant_id, product_no);

DROP INDEX public.uq_coa_system_role;
CREATE UNIQUE INDEX uq_coa_system_role ON public.fnd_chart_of_accounts USING btree (tenant_id, system_role) WHERE (system_role IS NOT NULL);

COMMIT;

-- Sanity check after running:
--   SELECT count(*) FROM fnd_tenant;  -- should be 1
--   SELECT count(*) FROM app_sale WHERE tenant_id IS NULL;  -- should be 0 (repeat per table if paranoid)