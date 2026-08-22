package com.triasoft.garage;

import com.triasoft.garage.company.entity.Company;
import com.triasoft.garage.company.entity.UserCompanyAccess;
import com.triasoft.garage.company.repository.CompanyRepository;
import com.triasoft.garage.company.repository.UserCompanyAccessRepository;
import com.triasoft.garage.company.service.CompanyReportService;
import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.FndModule;
import com.triasoft.garage.entity.Resource;
import com.triasoft.garage.entity.Role;
import com.triasoft.garage.entity.RolePrivilege;
import com.triasoft.garage.entity.Tenant;
import com.triasoft.garage.entity.UserProfile;
import com.triasoft.garage.entity.Vendor;
import com.triasoft.garage.entity.Warehouse;
import com.triasoft.garage.ledger.entity.ChartOfAccount;
import com.triasoft.garage.ledger.repository.ChartOfAccountRepository;
import com.triasoft.garage.ledger.service.LedgerService;
import com.triasoft.garage.repository.FndModuleRepository;
import com.triasoft.garage.repository.ResourceRepository;
import com.triasoft.garage.repository.RolePrivilegeRepository;
import com.triasoft.garage.repository.RoleRepository;
import com.triasoft.garage.repository.TenantRepository;
import com.triasoft.garage.repository.UserProfileRepository;
import com.triasoft.garage.repository.VendorRepository;
import com.triasoft.garage.repository.WarehouseRepository;
import com.triasoft.garage.security.rbac.PrivilegeCache;
import com.triasoft.garage.security.tenant.TenantContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tenant isolation checks against a real (throwaway) Postgres database - the same
 * multi-tenancy mechanism (Hibernate {@code @TenantId} for ORM paths, explicit tenant_id
 * predicates for native queries, per-tenant PrivilegeCache) verified ad hoc throughout the
 * refactor, now as a permanent, repeatable test.
 * <p>
 * Named {@code *IT} (not {@code *Test}) so Surefire's default {@code mvn test} does NOT pick it
 * up - this provisions and tears down its own database via {@code psql} (no Testcontainers/Docker
 * in this project), which is heavier and less portable than the rest of the suite. Run explicitly
 * with {@code mvn test -Dtest=TenantIsolationIT}. Requires a local Postgres reachable the same way
 * {@code application-dev.yml} expects (see CLAUDE.md), with a {@code postgres} superuser role
 * psql can connect to without a password prompt.
 */
@SpringBootTest
class TenantIsolationIT {

    private static final String DB_NAME = "garage_tenant_isolation_it";

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        String url = "jdbc:postgresql://localhost:5432/" + DB_NAME;
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.jdbc-url", () -> url);
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @BeforeAll
    static void provisionDatabase() throws IOException, InterruptedException {
        runPsql("postgres", "DROP DATABASE IF EXISTS " + DB_NAME + ";");
        runPsql("postgres", "CREATE DATABASE " + DB_NAME + ";");
        runPsql(DB_NAME, "CREATE EXTENSION IF NOT EXISTS pg_trgm;");
        runPsqlFile(DB_NAME, "scripts/database-ddl.sql");
    }

    @AfterAll
    static void dropDatabase() throws IOException, InterruptedException {
        // The Spring context's own connection pool may still hold open connections at this
        // point (JUnit doesn't close the context until after @AfterAll returns) - terminate
        // them first or the DROP fails with "being accessed by other users".
        runPsql("postgres", "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '" + DB_NAME + "' AND pid <> pg_backend_pid();");
        runPsql("postgres", "DROP DATABASE IF EXISTS " + DB_NAME + ";");
    }

    private static void runPsql(String database, String sql) throws IOException, InterruptedException {
        run(new ProcessBuilder("psql", "-h", "localhost", "-U", "postgres", "-d", database, "-v", "ON_ERROR_STOP=1", "-c", sql));
    }

    private static void runPsqlFile(String database, String scriptPath) throws IOException, InterruptedException {
        run(new ProcessBuilder("psql", "-h", "localhost", "-U", "postgres", "-d", database, "-v", "ON_ERROR_STOP=1", "-f", scriptPath));
    }

    private static void run(ProcessBuilder builder) throws IOException, InterruptedException {
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("Command failed (" + String.join(" ", builder.command()) + "):\n" + output);
        }
    }

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private ChartOfAccountRepository chartOfAccountRepository;
    @Autowired
    private VendorRepository vendorRepository;
    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RolePrivilegeRepository rolePrivilegeRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private FndModuleRepository fndModuleRepository;
    @Autowired
    private PrivilegeCache privilegeCache;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserCompanyAccessRepository userCompanyAccessRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private LedgerService ledgerService;
    @Autowired
    private CompanyReportService companyReportService;

    private Long createTenant(String code) {
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(code);
        tenant.setStatus("ACTIVE");
        tenant.setCreatedBy(0L);
        tenant.setCreatedAt(java.time.LocalDateTime.now());
        return tenantRepository.save(tenant).getId();
    }

    // Caller must have TenantContext already set — Company is @TenantId-scoped.
    private Long createCompany(String code) {
        Company company = new Company();
        company.setCode(code);
        company.setName(code);
        company.setActive(true);
        company.setCreatedBy(0L);
        company.setCreatedAt(java.time.LocalDateTime.now());
        return companyRepository.save(company).getId();
    }

    /**
     * ORM path (Hibernate @TenantId): two tenants each create a ChartOfAccount with the SAME
     * code - allowed since the unique constraint is now (tenant_id, code) - and each tenant's
     * findAll() must see only its own row.
     */
    @Test
    void ormPathIsolatesFindAllAcrossTenants() {
        Long tenantA = createTenant("ORM_TENANT_A");
        Long tenantB = createTenant("ORM_TENANT_B");

        TenantContext.set(tenantA);
        Long companyA = createCompany("MAIN");
        ChartOfAccount coaA = newCoa("SAME_CODE", "Tenant A Cash", companyA);
        chartOfAccountRepository.save(coaA);
        TenantContext.clear();

        TenantContext.set(tenantB);
        Long companyB = createCompany("MAIN");
        ChartOfAccount coaB = newCoa("SAME_CODE", "Tenant B Cash", companyB);
        chartOfAccountRepository.save(coaB);
        TenantContext.clear();

        TenantContext.set(tenantA);
        List<ChartOfAccount> seenByA = chartOfAccountRepository.findAll();
        TenantContext.clear();

        assertThat(seenByA).extracting(ChartOfAccount::getName).containsExactly("Tenant A Cash");
    }

    /**
     * ORM path (Hibernate @TenantId): inf_warehouse was left out of the original tenant-bucketing
     * pass and retrofitted afterward (see multi-tenancy refactor notes). Same shape as the CoA
     * check above - two tenants each create a Warehouse with the SAME code, allowed since the
     * unique constraint is now (tenant_id, code), and each tenant's findAll() must see only its
     * own row.
     */
    @Test
    void warehouseOrmPathIsolatesFindAllAcrossTenants() {
        Long tenantA = createTenant("WAREHOUSE_TENANT_A");
        Long tenantB = createTenant("WAREHOUSE_TENANT_B");

        TenantContext.set(tenantA);
        Long companyA = createCompany("MAIN");
        warehouseRepository.save(newWarehouse("MAIN", "Tenant A Warehouse", companyA));
        TenantContext.clear();

        TenantContext.set(tenantB);
        Long companyB = createCompany("MAIN");
        warehouseRepository.save(newWarehouse("MAIN", "Tenant B Warehouse", companyB));
        TenantContext.clear();

        TenantContext.set(tenantA);
        List<Warehouse> seenByA = warehouseRepository.findAll();
        TenantContext.clear();

        assertThat(seenByA).extracting(Warehouse::getName).containsExactly("Tenant A Warehouse");
    }

    /**
     * Native-query path: VendorRepository.findVendorsWithOutstandingBalance has an explicit
     * tenant_id predicate (Hibernate's @TenantId does not apply to nativeQuery=true). Two
     * tenants each create a Vendor with the SAME mobile number - allowed since mobile_uk is now
     * (tenant_id, mobile) - and the native query must only surface the caller's own vendor.
     */
    @Test
    void nativeQueryPathIsolatesAcrossTenants() {
        Long tenantA = createTenant("NATIVE_TENANT_A");
        Long tenantB = createTenant("NATIVE_TENANT_B");

        TenantContext.set(tenantA);
        vendorRepository.save(newVendor("Vendor A", "9999999999"));
        TenantContext.clear();

        TenantContext.set(tenantB);
        vendorRepository.save(newVendor("Vendor B", "9999999999"));
        TenantContext.clear();

        var pageA = vendorRepository.findVendorsWithOutstandingBalance(tenantA, Pageable.unpaged());

        assertThat(pageA.getContent()).extracting("name").containsExactly("Vendor A");
    }

    /**
     * RBAC path: two tenants each define a role with the SAME code (ADMIN) and different
     * grants. PrivilegeCache must never let tenant B's check see tenant A's grant for the
     * identically-coded role, and vice versa. Mirrors the nested-transaction bug found and
     * fixed during Phase 3 - PrivilegeCache.refresh() must build a genuinely independent
     * grant set per tenant.
     */
    @Test
    void privilegeCacheIsolatesIdenticallyCodedRolesAcrossTenants() {
        Long tenantA = createTenant("RBAC_TENANT_A");
        Long tenantB = createTenant("RBAC_TENANT_B");

        // Resource/FndModule are global (Bucket B, no @TenantId) - database-ddl.sql only
        // creates the schema, not seed data, so create one here rather than assume it exists.
        FndModule module = new FndModule();
        module.setCode("TEST_MODULE_" + System.nanoTime());
        module.setDescription("Test Module");
        module.setActive(true);
        fndModuleRepository.save(module);
        Resource resource = new Resource();
        resource.setModule(module);
        resource.setCode("TEST_RESOURCE_" + System.nanoTime());
        resource.setDescription("Test Resource");
        resource.setActive(true);
        resourceRepository.save(resource);
        Long resourceId = resource.getId();
        String resourceCode = resource.getCode();

        TenantContext.set(tenantA);
        Role roleA = newRole("ADMIN");
        roleRepository.save(roleA);
        RolePrivilege grantA = new RolePrivilege();
        grantA.setRoleId(roleA.getId());
        grantA.setResourceId(resourceId);
        grantA.setPrivilege(Privilege.CREATE);
        grantA.setTenantId(tenantA);
        rolePrivilegeRepository.save(grantA);
        TenantContext.clear();

        TenantContext.set(tenantB);
        Role roleB = newRole("ADMIN");
        roleRepository.save(roleB);
        // Tenant B's ADMIN gets no grants at all.
        TenantContext.clear();

        privilegeCache.refresh();

        boolean tenantAGranted = privilegeCache.isGranted(tenantA, List.of("ADMIN"), resourceCode, Privilege.CREATE);
        boolean tenantBGranted = privilegeCache.isGranted(tenantB, List.of("ADMIN"), resourceCode, Privilege.CREATE);

        assertThat(tenantAGranted).isTrue();
        assertThat(tenantBGranted).isFalse();
    }

    /**
     * ORM path (Hibernate @TenantId): Company itself, same shape as the CoA/Warehouse
     * checks above — two tenants each create a Company with the SAME code (allowed since
     * the unique constraint is (tenant_id, code)), and each tenant's findAll() must see
     * only its own row.
     */
    @Test
    void companyOrmPathIsolatesFindAllAcrossTenants() {
        Long tenantA = createTenant("COMPANY_TENANT_A");
        Long tenantB = createTenant("COMPANY_TENANT_B");

        TenantContext.set(tenantA);
        companyRepository.save(newCompany("MAIN", "Tenant A Company"));
        TenantContext.clear();

        TenantContext.set(tenantB);
        companyRepository.save(newCompany("MAIN", "Tenant B Company"));
        TenantContext.clear();

        TenantContext.set(tenantA);
        List<Company> seenByA = companyRepository.findAllByOrderByIdAsc();
        TenantContext.clear();

        assertThat(seenByA).extracting(Company::getName).containsExactly("Tenant A Company");
    }

    /**
     * Company-level scoping (application-managed, NOT a second Hibernate @TenantId — see the
     * multi-company plan for why): within a SINGLE tenant, two companies each get their own
     * AR-role ChartOfAccount row with a different name. LedgerService.findAccountBySystemRole
     * must resolve the correct company's row, never the other company's — this is the load-
     * bearing check for the whole "books never blend across companies" guarantee, since this
     * scoping is plain application code, not something Hibernate enforces automatically the
     * way @TenantId does for tenant_id.
     */
    @Test
    void chartOfAccountSystemRoleScopedByCompanyWithinSameTenant() {
        Long tenant = createTenant("COA_COMPANY_TENANT");
        TenantContext.set(tenant);
        Long companyA = createCompany("COMPANY_A");
        Long companyB = createCompany("COMPANY_B");

        ChartOfAccount arA = newCoa("1100", "Company A AR", companyA);
        arA.setSystemRole("AR");
        chartOfAccountRepository.save(arA);

        ChartOfAccount arB = newCoa("1100", "Company B AR", companyB);
        arB.setSystemRole("AR");
        chartOfAccountRepository.save(arB);

        ChartOfAccount resolvedForA = ledgerService.findAccountBySystemRole("AR", companyA);
        ChartOfAccount resolvedForB = ledgerService.findAccountBySystemRole("AR", companyB);
        TenantContext.clear();

        assertThat(resolvedForA.getName()).isEqualTo("Company A AR");
        assertThat(resolvedForB.getName()).isEqualTo("Company B AR");
    }

    /**
     * Access-grant gating: a non-SUPERADMIN user granted access to only ONE of a tenant's two
     * companies must not see the other company in the cross-company comparison report — the
     * whole point of user_company_access.
     */
    @Test
    void companyReportServiceOnlyIncludesGrantedCompanies() {
        Long tenant = createTenant("REPORT_ACCESS_TENANT");
        TenantContext.set(tenant);
        Long companyA = createCompany("REPORT_A");
        Long companyB = createCompany("REPORT_B");

        UserProfile user = new UserProfile();
        user.setTenantId(tenant); // TenantScopedEntity — plain column, not @TenantId, must set explicitly
        user.setName("Report Tester");
        user.setUsername("report_tester_" + System.nanoTime());
        user.setPassword("x");
        user.setRole("STAFF");
        user = userProfileRepository.save(user);

        UserCompanyAccess access = new UserCompanyAccess();
        access.setUserId(user.getId());
        access.setCompanyId(companyA);
        userCompanyAccessRepository.save(access);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setTenantId(tenant);
        userDTO.setRoles(List.of("STAFF"));

        var result = companyReportService.compare(java.time.YearMonth.now(), userDTO);
        TenantContext.clear();

        assertThat(result.getCompanies()).extracting("companyId").containsExactly(companyA);
    }

    private Company newCompany(String code, String name) {
        Company company = new Company();
        company.setCode(code);
        company.setName(name);
        company.setActive(true);
        company.setCreatedBy(0L);
        company.setCreatedAt(java.time.LocalDateTime.now());
        return company;
    }

    private ChartOfAccount newCoa(String code, String name, Long companyId) {
        ChartOfAccount coa = new ChartOfAccount();
        coa.setCompanyId(companyId);
        coa.setCode(code);
        coa.setName(name);
        coa.setLabel(name);
        coa.setDescription(name);
        coa.setType("ASSET");
        coa.setControlEnabled(false);
        coa.setDirectPostable(true);
        coa.setCreatedBy(0L);
        coa.setCreatedAt(java.time.LocalDateTime.now());
        return coa;
    }

    private Vendor newVendor(String name, String mobile) {
        Vendor vendor = new Vendor();
        vendor.setName(name);
        vendor.setMobile(mobile);
        vendor.setCreatedBy(0L);
        vendor.setCreatedAt(java.time.LocalDateTime.now());
        return vendor;
    }

    private Warehouse newWarehouse(String code, String name, Long companyId) {
        Warehouse warehouse = new Warehouse();
        warehouse.setCompanyId(companyId);
        warehouse.setCode(code);
        warehouse.setName(name);
        warehouse.setCreatedBy(0L);
        warehouse.setCreatedAt(java.time.LocalDateTime.now());
        return warehouse;
    }

    private Role newRole(String code) {
        Role role = new Role();
        role.setCode(code);
        role.setName(code);
        role.setSystem(false);
        role.setCreatedBy(0L);
        role.setCreatedAt(java.time.LocalDateTime.now());
        return role;
    }
}
