INSERT INTO fnd_tenant (code, name, status, created_by, created_at, version)
VALUES ('FUTURE_CARS', 'Future Cars', 'ACTIVE', 0, now(), 0);

INSERT INTO app_company (tenant_id, code, name, registration_no, address, active, created_by, created_at, version)
VALUES (1, 'FUTURE_CARS', 'Future Cars', NULL, NULL, true, 0, now(), 0);

INSERT INTO inf_warehouse (tenant_id, company_id, code, name, created_by, created_at, version) VALUES
(1, 1, 'MAIN', 'Main Warehouse', 0, now(), 0);

INSERT INTO warehouse_business_line (tenant_id, warehouse_id, business_line)
VALUES (1, 1, 'VEHICLE_SALES');

INSERT INTO fnd_chart_of_accounts
  (tenant_id, company_id, code, name, description, type, is_control_account, label, is_direct_postable, system_role, created_by, created_at, version) VALUES
  (1, 1, '3000', 'E-OE',         'Owner''s capital contributions to the business.',                                'EQUITY',     true,  'Owner''s Equity',                                    false, 'OWNERS_INVESTMENT',                          0, now(), 0),
  (1, 1, '1100', 'E-OD',         'Withdrawals by the owner from the business.',                                'EQUITY',     true,  'Owner''s Drawing',                                    false, 'OWNERS_DRAWINGS',                          0, now(), 0),
  (1, 1, '1100', 'A-ACR',         'Money owed to the company by customers. (Control Account)',                                'ASSET',     true,  'Accounts Receivable (A/R)',                                    false, 'AR',                          0, now(), 0),
  (1, 1, '1150', 'A-FR',          'Amount due from finance companies for financed vehicle sales.',                            'ASSET',     false, 'Finance Receivable',                                           false, 'FINANCE_RECEIVABLE',          0, now(), 0),
  (1, 1, '1170', 'VND-REF-REC',   'Vendor Refund Receivable',                                                                 'ASSET',     true,  'Money owed to us by vendors for returned purchases',           false, 'VENDOR_REFUND_RECEIVABLE',    0, now(), 0),
  (1, 1, '1200', 'A-INV',         'Total cost of all vehicles in stock. (Control Account)',                                   'ASSET',     true,  'Inventory - Vehicles',                                         false, 'INVENTORY',                   0, now(), 0),
  (1, 1, '2000', 'L-ACP',         'Money owed by the company to vendors/suppliers. (Control Account)',                        'LIABILITY', true,  'Accounts Payable (A/P)',                                       false, 'AP',                          0, now(), 0),
  (1, 1, '2400', 'L-CSP',         'Amounts owed to customers from trade-in exchanges where exchange value exceeds sale value.','LIABILITY', false, 'Customer Settlement Payable',                                  false, 'CUSTOMER_SETTLEMENT_PAYABLE', 0, now(), 0),
  (1, 1, '2410', 'CUST-REF-PAY',  'Customer Refund Payable',                                                                  'LIABILITY', true,  'Money owed to customers for returned sales, pending refund',   false, 'CUSTOMER_REFUND_PAYABLE',     0, now(), 0),
  (1, 1, '3900', 'E-OBE',         'Temporary clearing account for opening balances during initial setup.',                    'EQUITY',    false, 'Opening Balance Equity',                                       false, 'OPENING_BALANCE_EQUITY',      0, now(), 0),
  (1, 1, '4000', 'R-IN',          'Gross revenue from the sale of new and used vehicles.',                                     'REVENUE',   false, 'Sales Revenue - Vehicles',                                     false, 'SALES_REVENUE',               0, now(), 0),
  (1, 1, '4010', 'R-SVC',         'Revenue from standalone services (e.g. car wash) not tied to a vehicle sale.',              'REVENUE',   false, 'Service Revenue',                                              false, 'SERVICE_REVENUE',             0, now(), 0),
  (1, 1, '4520', 'RTN-INC-DED',   'Return Deduction Income',                                                                  'REVENUE',   true,  'Return Deduction Income',                                       false, 'RETURN_DEDUCTION_INCOME',     0, now(), 0),
  (1, 1, '4530', 'RTN-INC-EXGAIN','Gain on Exchange Adjustment',                                                              'REVENUE',   true,  'Gain on Exchange Adjustment',                                   false, 'GAIN_ON_EXCHANGE_ADJ',        0, now(), 0),
  (1, 1, '5000', 'CGOS-IN',       'The landed cost of vehicles that have been sold.',                                         'EXPENSE',   false, 'COGS - Vehicles',                                              false, 'COGS',                        0, now(), 0),
  (1, 1, '5100', 'E-SAL',         'Salaries paid to employees.',                                                              'EXPENSE',   false, 'Salary Expense',                                               false, 'SALARY_EXPENSE',              0, now(), 0),
  (1, 1, '5510', 'RTN-EXP-EXCH',  'Loss on Returned Exchange Vehicle',                                                        'EXPENSE',   true,  'Loss on Returned Exchange Vehicle',                            false, 'LOSS_RETURNED_EXCHANGE',      0, now(), 0),
  (1, 1, '5520', 'RTN-EXP-PUR',   'Loss on Purchase Return',                                                                  'EXPENSE',   true,  'Loss on Purchase Return',                                      false, 'LOSS_PURCHASE_RETURN',        0, now(), 0);

INSERT INTO public.fnd_role OVERRIDING SYSTEM VALUE VALUES (1,1, 'SUPERADMIN', 'Super Admin', 'Unrestricted access; bypasses privilege checks entirely', true, false, NULL, '2026-07-22 01:17:07.875997', NULL, NULL, 0);
INSERT INTO public.fnd_role OVERRIDING SYSTEM VALUE VALUES (2,1, 'ADMIN', 'Admin', 'Administrative access; privileges granted explicitly like any other role', true, false, NULL, '2026-07-22 01:17:07.875997', NULL, NULL, 0);

INSERT INTO user_profile (tenant_id, username, password, name, designation, deleted)
VALUES (1, 'superadmin', '$2a$10$.4eLvOnwMk4DXnxV.ahiQO5e1tTLU8NrZCyyT76b77fa2Bsc6ebGy', 'SuperAdmin', 'SUPERADMIN', false);

INSERT INTO user_role (tenant_id, user_id, role_id) VALUES (1,1,1);