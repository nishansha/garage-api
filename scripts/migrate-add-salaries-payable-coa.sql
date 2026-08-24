-- ============================================================================
-- Payroll is moving from cash-basis to accrual journal posting: a SalaryPayment
-- will now post Dr SALARY_EXPENSE / Cr SALARIES_PAYABLE at generation time (not
-- just a single entry at markPaid, as before). This requires a SALARIES_PAYABLE
-- system-role row in fnd_chart_of_accounts for every company - JournalService.coa()
-- looks these up by (system_role, company_id) and throws BUS_130 if missing.
-- This backfills the row for every company that already exists.
-- ============================================================================
-- Run this ONCE, after migrate-to-multi-company.sql has already been applied,
-- and BEFORE deploying the code that posts SALARY_ACCRUAL journals - if even one
-- company in a tenant is missing this row, SalaryRunWorkUnit's per-tenant
-- REQUIRES_NEW transaction will roll back that tenant's ENTIRE monthly salary
-- run, not just the affected company. Take a database backup first - see
-- scripts/backup.sh.
-- ============================================================================

BEGIN;

INSERT INTO public.fnd_chart_of_accounts
    (tenant_id, company_id, code, name, description, type, is_control_account, label, is_direct_postable, system_role, created_by, created_at, version)
SELECT c.tenant_id, c.id, '2420', 'L-SAL-PAY', 'Salaries earned but not yet paid out.', 'LIABILITY', false, 'Salaries Payable', false, 'SALARIES_PAYABLE', 0, now(), 0
FROM public.app_company c
WHERE NOT EXISTS (
    SELECT 1 FROM public.fnd_chart_of_accounts coa
    WHERE coa.company_id = c.id AND coa.system_role = 'SALARIES_PAYABLE'
);

COMMIT;

-- Sanity check after running (should return 0 rows):
--   SELECT c.id, c.code FROM public.app_company c
--   WHERE NOT EXISTS (
--       SELECT 1 FROM public.fnd_chart_of_accounts coa
--       WHERE coa.company_id = c.id AND coa.system_role = 'SALARIES_PAYABLE'
--   );
