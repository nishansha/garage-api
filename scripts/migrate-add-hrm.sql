-- ============================================================================
-- Payroll (HRM) module: adds app_employee / app_salary_payment (brand new
-- tables, no backfill needed) plus the EMPLOYEE/SALARY_PAYMENT RBAC resources.
-- ============================================================================
-- Run this ONCE, after migrate-to-multi-company.sql has already been applied
-- (these tables reference app_company's company_id). Take a database backup
-- first - see scripts/backup.sh.
-- ============================================================================

BEGIN;

CREATE SEQUENCE IF NOT EXISTS public.app_employee_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE public.app_employee (
    id bigint DEFAULT nextval('public.app_employee_id_seq'::regclass) NOT NULL,
    tenant_id bigint NOT NULL,
    company_id bigint NOT NULL,
    employee_code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    designation character varying(100),
    join_date date NOT NULL,
    termination_date date,
    salary_amount numeric(15,2) NOT NULL,
    bank_name character varying(100),
    bank_account_no character varying(50),
    payment_account_id bigint NOT NULL,
    user_profile_id bigint,
    active boolean DEFAULT true NOT NULL,
    deleted boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone,
    created_by bigint,
    modified_at timestamp without time zone,
    modified_by bigint,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT app_employee_pkey PRIMARY KEY (id),
    CONSTRAINT app_employee_uk1 UNIQUE (tenant_id, company_id, employee_code),
    CONSTRAINT app_employee_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id),
    CONSTRAINT app_employee_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.app_company(id),
    CONSTRAINT app_employee_payment_account_id_fkey FOREIGN KEY (payment_account_id) REFERENCES public.app_payment_account(id),
    CONSTRAINT app_employee_user_profile_id_fkey FOREIGN KEY (user_profile_id) REFERENCES public.user_profile(id)
);

CREATE SEQUENCE IF NOT EXISTS public.app_salary_payment_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE public.app_salary_payment (
    id bigint DEFAULT nextval('public.app_salary_payment_id_seq'::regclass) NOT NULL,
    tenant_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    pay_period_month integer NOT NULL,
    pay_period_year integer NOT NULL,
    gross_amount numeric(15,2) NOT NULL,
    net_amount numeric(15,2) NOT NULL,
    payment_date date,
    payment_account_id bigint,
    status character varying(20) NOT NULL,
    notes character varying(500),
    created_at timestamp without time zone,
    created_by bigint,
    modified_at timestamp without time zone,
    modified_by bigint,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT app_salary_payment_pkey PRIMARY KEY (id),
    CONSTRAINT app_salary_payment_uk1 UNIQUE (employee_id, pay_period_month, pay_period_year),
    CONSTRAINT app_salary_payment_tenant_id_fkey FOREIGN KEY (tenant_id) REFERENCES public.fnd_tenant(id),
    CONSTRAINT app_salary_payment_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES public.app_employee(id),
    CONSTRAINT app_salary_payment_payment_account_id_fkey FOREIGN KEY (payment_account_id) REFERENCES public.app_payment_account(id)
);

-- RBAC resources for the new screens/endpoints.
INSERT INTO public.fnd_resource (id, module_id, code, description, sort_order, active)
OVERRIDING SYSTEM VALUE
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM public.fnd_resource), 9, 'EMPLOYEE', 'Employees', 16, true
WHERE NOT EXISTS (SELECT 1 FROM public.fnd_resource WHERE code = 'EMPLOYEE');

INSERT INTO public.fnd_resource (id, module_id, code, description, sort_order, active)
OVERRIDING SYSTEM VALUE
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM public.fnd_resource), 9, 'SALARY_PAYMENT', 'Payroll', 17, true
WHERE NOT EXISTS (SELECT 1 FROM public.fnd_resource WHERE code = 'SALARY_PAYMENT');

-- SALARY_EXPENSE Chart-of-Account row for every existing company (new tenants get this
-- automatically from the updated provision-company-template.sql, but companies
-- provisioned before that update need it backfilled).
INSERT INTO public.fnd_chart_of_accounts
  (tenant_id, company_id, code, name, description, type, is_control_account, label, is_direct_postable, system_role, created_by, created_at, version)
SELECT c.tenant_id, c.id, '5100', 'E-SAL', 'Salaries paid to employees.', 'EXPENSE', false, 'Salary Expense', false, 'SALARY_EXPENSE', 0, now(), 0
FROM public.app_company c
WHERE NOT EXISTS (
    SELECT 1 FROM public.fnd_chart_of_accounts coa
    WHERE coa.company_id = c.id AND coa.system_role = 'SALARY_EXPENSE'
);

COMMIT;

-- Sanity check after running:
--   SELECT * FROM fnd_resource WHERE code IN ('EMPLOYEE', 'SALARY_PAYMENT');
--   SELECT company_id FROM fnd_chart_of_accounts WHERE system_role = 'SALARY_EXPENSE';
--   -- should list every row in app_company
