-- ============================================================================
-- Payment accounts (bank/cash) were never given a company_id during the
-- original multi-company migration - PaymentAccountService.autoCreateCoA()
-- creates a company-scoped fnd_chart_of_accounts row for every payment
-- account but never set its company_id, which fails the NOT NULL constraint
-- added by migrate-to-multi-company.sql. This backfills existing rows and
-- adds the missing column/constraint.
-- ============================================================================
-- Run this ONCE, after migrate-to-multi-company.sql has already been applied.
-- Take a database backup first - see scripts/backup.sh.
-- ============================================================================

BEGIN;

ALTER TABLE public.app_payment_account ADD COLUMN company_id bigint;

-- Every existing payment account already links to a company-scoped CoA row
-- (set during the original multi-company backfill) - derive from that.
-- Fall back to the tenant's default company for the rare case of a payment
-- account whose coa_id was never set.
UPDATE public.app_payment_account pa
SET company_id = COALESCE(
    (SELECT coa.company_id FROM public.fnd_chart_of_accounts coa WHERE coa.id = pa.coa_id),
    (SELECT c.id FROM public.app_company c WHERE c.tenant_id = pa.tenant_id AND c.code = 'MAIN')
);

ALTER TABLE public.app_payment_account ALTER COLUMN company_id SET NOT NULL;

ALTER TABLE public.app_payment_account
    ADD CONSTRAINT app_payment_account_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.app_company(id);

COMMIT;

-- Sanity check after running:
--   SELECT count(*) FROM app_payment_account WHERE company_id IS NULL;  -- should be 0
--   SELECT pa.id, pa.name, pa.company_id, coa.company_id AS coa_company_id
--   FROM app_payment_account pa LEFT JOIN fnd_chart_of_accounts coa ON coa.id = pa.coa_id
--   WHERE pa.company_id IS DISTINCT FROM coa.company_id;  -- should be empty (or only the fallback rows)
