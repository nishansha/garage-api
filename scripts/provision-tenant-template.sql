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

-- 3. System-role Chart of Accounts ----------------------------------------
-- REQUIRED, not optional: JournalService/ReportService look these up by
-- system_role (SystemCoaRole enum) by name. Every tenant must have exactly
-- these 14 rows or postings/reports for that tenant will fail immediately.
INSERT INTO fnd_chart_of_accounts
  (tenant_id, code, name, description, type, is_control_account, label, is_direct_postable, system_role, created_by, created_at, version) VALUES
  (:tenant_id, '1100', 'A-ACR',         'Money owed to the company by customers. (Control Account)',                                'ASSET',     true,  'Accounts Receivable (A/R)',                                    false, 'AR',                          0, now(), 0),
  (:tenant_id, '1150', 'A-FR',          'Amount due from finance companies for financed vehicle sales.',                            'ASSET',     false, 'Finance Receivable',                                           false, 'FINANCE_RECEIVABLE',          0, now(), 0),
  (:tenant_id, '1170', 'VND-REF-REC',   'Vendor Refund Receivable',                                                                 'ASSET',     true,  'Money owed to us by vendors for returned purchases',           false, 'VENDOR_REFUND_RECEIVABLE',    0, now(), 0),
  (:tenant_id, '1200', 'A-INV',         'Total cost of all vehicles in stock. (Control Account)',                                   'ASSET',     true,  'Inventory - Vehicles',                                         false, 'INVENTORY',                   0, now(), 0),
  (:tenant_id, '2000', 'L-ACP',         'Money owed by the company to vendors/suppliers. (Control Account)',                        'LIABILITY', true,  'Accounts Payable (A/P)',                                       false, 'AP',                          0, now(), 0),
  (:tenant_id, '2400', 'L-CSP',         'Amounts owed to customers from trade-in exchanges where exchange value exceeds sale value.','LIABILITY', false, 'Customer Settlement Payable',                                  false, 'CUSTOMER_SETTLEMENT_PAYABLE', 0, now(), 0),
  (:tenant_id, '2410', 'CUST-REF-PAY',  'Customer Refund Payable',                                                                  'LIABILITY', true,  'Money owed to customers for returned sales, pending refund',   false, 'CUSTOMER_REFUND_PAYABLE',     0, now(), 0),
  (:tenant_id, '3900', 'E-OBE',         'Temporary clearing account for opening balances during initial setup.',                    'EQUITY',    false, 'Opening Balance Equity',                                       false, 'OPENING_BALANCE_EQUITY',      0, now(), 0),
  (:tenant_id, '4000', 'R-IN',          'Gross revenue from the sale of new and used vehicles.',                                     'REVENUE',   false, 'Sales Revenue - Vehicles',                                     false, 'SALES_REVENUE',               0, now(), 0),
  (:tenant_id, '4520', 'RTN-INC-DED',   'Return Deduction Income',                                                                  'REVENUE',   true,  'Return Deduction Income',                                       false, 'RETURN_DEDUCTION_INCOME',     0, now(), 0),
  (:tenant_id, '4530', 'RTN-INC-EXGAIN','Gain on Exchange Adjustment',                                                              'REVENUE',   true,  'Gain on Exchange Adjustment',                                   false, 'GAIN_ON_EXCHANGE_ADJ',        0, now(), 0),
  (:tenant_id, '5000', 'CGOS-IN',       'The landed cost of vehicles that have been sold.',                                         'EXPENSE',   false, 'COGS - Vehicles',                                              false, 'COGS',                        0, now(), 0),
  (:tenant_id, '5510', 'RTN-EXP-EXCH',  'Loss on Returned Exchange Vehicle',                                                        'EXPENSE',   true,  'Loss on Returned Exchange Vehicle',                            false, 'LOSS_RETURNED_EXCHANGE',      0, now(), 0),
  (:tenant_id, '5520', 'RTN-EXP-PUR',   'Loss on Purchase Return',                                                                  'EXPENSE',   true,  'Loss on Purchase Return',                                      false, 'LOSS_PURCHASE_RETURN',        0, now(), 0);

-- 4. First user (this tenant's own SUPERADMIN) -----------------------------
INSERT INTO user_profile (tenant_id, username, password, name, designation, deleted)
VALUES (:tenant_id, 'USERNAME', 'PASSWORD_BCRYPT_HASH', 'Full Name', 'Owner', false)
RETURNING id AS user_id \gset

INSERT INTO user_role (tenant_id, user_id, role_id)
SELECT :tenant_id, :user_id, id FROM fnd_role WHERE tenant_id = :tenant_id AND code = 'SUPERADMIN';

COMMIT;

-- Sanity check after running:
--   SELECT * FROM fnd_tenant WHERE id = :tenant_id;
--   SELECT * FROM fnd_role WHERE tenant_id = :tenant_id;
--   SELECT * FROM fnd_chart_of_accounts WHERE tenant_id = :tenant_id ORDER BY code;
--   SELECT up.username, r.code FROM user_profile up
--     JOIN user_role ur ON ur.user_id = up.id JOIN fnd_role r ON r.id = ur.role_id
--     WHERE up.tenant_id = :tenant_id;
