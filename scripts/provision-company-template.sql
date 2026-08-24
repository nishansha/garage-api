-- ============================================================================
-- Company provisioning template
-- ============================================================================
-- Backend-only, run-by-hand (psql -f, or paste into a psql session), same as
-- provision-tenant-template.sql. Run this once per company under an EXISTING
-- tenant — required immediately after provision-tenant-template.sql for a
-- brand new tenant (which has no Chart of Accounts/warehouse of its own until
-- a company exists), and again any time that tenant adds a second company
-- with genuinely separate books (e.g. a different legal entity/partners).
--
-- Set :tenant_id before running, e.g.:
--   psql -v tenant_id=5 -f scripts/provision-company-template.sql
-- or, if run in the same psql session right after provision-tenant-template.sql,
-- :tenant_id is already set by that script's \gset.
--
-- Replace every ALL-CAPS placeholder below, then run the whole file as one
-- transaction. Uses psql's \gset to carry the generated company id between
-- statements, so run it with psql (not through a tool that only executes
-- plain SQL without meta-commands).
-- ============================================================================

BEGIN;

-- 1. Company ----------------------------------------------------------------
INSERT INTO app_company (tenant_id, code, name, registration_no, address, active, created_by, created_at, version)
VALUES (:tenant_id, 'COMPANY_CODE', 'Company Display Name', NULL, NULL, true, 0, now(), 0)
RETURNING id AS company_id \gset

-- 2. System-role Chart of Accounts ----------------------------------------
-- REQUIRED, not optional: JournalService/ReportService look these up by
-- system_role (SystemCoaRole enum) by name, scoped to (tenant_id, company_id).
-- Every company must have these rows or postings/reports for it will fail
-- immediately. NOTE: this is knowingly missing RC_DUE_RECEIVABLE — a
-- pre-existing gap in the original tenant-level version of this script,
-- carried forward as-is and tracked separately, not fixed here.
INSERT INTO fnd_chart_of_accounts
  (tenant_id, company_id, code, name, description, type, is_control_account, label, is_direct_postable, system_role, created_by, created_at, version) VALUES
  (:tenant_id, :company_id, '1100', 'A-ACR',         'Money owed to the company by customers. (Control Account)',                                'ASSET',     true,  'Accounts Receivable (A/R)',                                    false, 'AR',                          0, now(), 0),
  (:tenant_id, :company_id, '1150', 'A-FR',          'Amount due from finance companies for financed vehicle sales.',                            'ASSET',     false, 'Finance Receivable',                                           false, 'FINANCE_RECEIVABLE',          0, now(), 0),
  (:tenant_id, :company_id, '1170', 'VND-REF-REC',   'Vendor Refund Receivable',                                                                 'ASSET',     true,  'Money owed to us by vendors for returned purchases',           false, 'VENDOR_REFUND_RECEIVABLE',    0, now(), 0),
  (:tenant_id, :company_id, '1200', 'A-INV',         'Total cost of all vehicles in stock. (Control Account)',                                   'ASSET',     true,  'Inventory - Vehicles',                                         false, 'INVENTORY',                   0, now(), 0),
  (:tenant_id, :company_id, '2000', 'L-ACP',         'Money owed by the company to vendors/suppliers. (Control Account)',                        'LIABILITY', true,  'Accounts Payable (A/P)',                                       false, 'AP',                          0, now(), 0),
  (:tenant_id, :company_id, '2400', 'L-CSP',         'Amounts owed to customers from trade-in exchanges where exchange value exceeds sale value.','LIABILITY', false, 'Customer Settlement Payable',                                  false, 'CUSTOMER_SETTLEMENT_PAYABLE', 0, now(), 0),
  (:tenant_id, :company_id, '2410', 'CUST-REF-PAY',  'Customer Refund Payable',                                                                  'LIABILITY', true,  'Money owed to customers for returned sales, pending refund',   false, 'CUSTOMER_REFUND_PAYABLE',     0, now(), 0),
  (:tenant_id, :company_id, '2420', 'L-SAL-PAY',     'Salaries earned but not yet paid out.',                                                    'LIABILITY', false, 'Salaries Payable',                                             false, 'SALARIES_PAYABLE',            0, now(), 0),
  (:tenant_id, :company_id, '3900', 'E-OBE',         'Temporary clearing account for opening balances during initial setup.',                    'EQUITY',    false, 'Opening Balance Equity',                                       false, 'OPENING_BALANCE_EQUITY',      0, now(), 0),
  (:tenant_id, :company_id, '4000', 'R-IN',          'Gross revenue from the sale of new and used vehicles.',                                     'REVENUE',   false, 'Sales Revenue - Vehicles',                                     false, 'SALES_REVENUE',               0, now(), 0),
  (:tenant_id, :company_id, '4010', 'R-SVC',         'Revenue from standalone services (e.g. car wash) not tied to a vehicle sale.',              'REVENUE',   false, 'Service Revenue',                                              false, 'SERVICE_REVENUE',             0, now(), 0),
  (:tenant_id, :company_id, '4520', 'RTN-INC-DED',   'Return Deduction Income',                                                                  'REVENUE',   true,  'Return Deduction Income',                                       false, 'RETURN_DEDUCTION_INCOME',     0, now(), 0),
  (:tenant_id, :company_id, '4530', 'RTN-INC-EXGAIN','Gain on Exchange Adjustment',                                                              'REVENUE',   true,  'Gain on Exchange Adjustment',                                   false, 'GAIN_ON_EXCHANGE_ADJ',        0, now(), 0),
  (:tenant_id, :company_id, '5000', 'CGOS-IN',       'The landed cost of vehicles that have been sold.',                                         'EXPENSE',   false, 'COGS - Vehicles',                                              false, 'COGS',                        0, now(), 0),
  (:tenant_id, :company_id, '5100', 'E-SAL',         'Salaries paid to employees.',                                                              'EXPENSE',   false, 'Salary Expense',                                               false, 'SALARY_EXPENSE',              0, now(), 0),
  (:tenant_id, :company_id, '5510', 'RTN-EXP-EXCH',  'Loss on Returned Exchange Vehicle',                                                        'EXPENSE',   true,  'Loss on Returned Exchange Vehicle',                            false, 'LOSS_RETURNED_EXCHANGE',      0, now(), 0),
  (:tenant_id, :company_id, '5520', 'RTN-EXP-PUR',   'Loss on Purchase Return',                                                                  'EXPENSE',   true,  'Loss on Purchase Return',                                      false, 'LOSS_PURCHASE_RETURN',        0, now(), 0);

-- 3. Default warehouse ------------------------------------------------------
-- Warehouse now belongs to a company, not just a tenant. Seed one so
-- purchases/service-sales have somewhere to assign to without requiring
-- warehouse management to happen first. Business-line capability (vehicle
-- sales vs. services) is configured afterward via the Warehouses screen/API.
INSERT INTO inf_warehouse (tenant_id, company_id, code, name, created_by, created_at, version)
VALUES (:tenant_id, :company_id, 'MAIN', 'Main Warehouse', 0, now(), 0)
RETURNING id AS warehouse_id \gset

-- Defaults to VEHICLE_SALES — add a SERVICES row too (or via the Warehouses screen
-- afterward) if this company also runs a standalone services business.
INSERT INTO warehouse_business_line (tenant_id, warehouse_id, business_line)
VALUES (:tenant_id, :warehouse_id, 'VEHICLE_SALES');

COMMIT;

-- Sanity check after running:
--   SELECT * FROM app_company WHERE id = :company_id;
--   SELECT * FROM fnd_chart_of_accounts WHERE company_id = :company_id ORDER BY code;
--   SELECT * FROM inf_warehouse WHERE company_id = :company_id;
--   SELECT * FROM warehouse_business_line WHERE warehouse_id = :warehouse_id;
