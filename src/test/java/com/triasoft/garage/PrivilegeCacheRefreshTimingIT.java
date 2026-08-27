package com.triasoft.garage;

import com.triasoft.garage.constants.Privilege;
import com.triasoft.garage.constants.TenantStatus;
import com.triasoft.garage.entity.FndModule;
import com.triasoft.garage.entity.Resource;
import com.triasoft.garage.entity.Role;
import com.triasoft.garage.entity.Tenant;
import com.triasoft.garage.model.role.PrivilegeGrantRq;
import com.triasoft.garage.model.role.RolePrivilegeRq;
import com.triasoft.garage.repository.FndModuleRepository;
import com.triasoft.garage.repository.ResourceRepository;
import com.triasoft.garage.repository.RoleRepository;
import com.triasoft.garage.repository.TenantRepository;
import com.triasoft.garage.security.rbac.PrivilegeCache;
import com.triasoft.garage.security.tenant.TenantContext;
import com.triasoft.garage.service.impl.RoleService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for a reported bug: after calling RoleService.updatePrivileges() through a
 * real request, the change doesn't take effect until the app is restarted. Root cause -
 * TenantScopedGrantLoader.loadForCurrentTenant() (which PrivilegeCache.refresh() calls per
 * tenant) is REQUIRES_NEW, and updatePrivileges()/delete() are themselves @Transactional and
 * call privilegeCache.refresh() BEFORE their own transaction commits. REQUIRES_NEW correctly
 * suspends the caller's transaction and opens a genuinely new one (unlike the OSIV-related bug
 * found in TenantProvisioningService, where no transaction was active yet to suspend) - but that
 * new transaction runs on a separate DB connection under the default READ COMMITTED isolation,
 * which cannot see the caller's own not-yet-committed writes. The cache silently gets rebuilt
 * with stale (pre-change) data every time, and only a restart (which rebuilds from committed
 * data) shows the correct grants.
 */
@SpringBootTest
class PrivilegeCacheRefreshTimingIT {

    private static final String DB_NAME = "garage_privilege_cache_refresh_it";

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
    private RoleRepository roleRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private FndModuleRepository fndModuleRepository;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PrivilegeCache privilegeCache;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void updatePrivileges_grantIsVisibleInCacheImmediatelyAfterTheCallReturns_notOnlyAfterRestart() {
        Tenant tenant = new Tenant();
        tenant.setCode("PRIV_REFRESH_TENANT");
        tenant.setName("Priv Refresh Tenant");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setCreatedBy(0L);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant = tenantRepository.save(tenant);
        Long tenantId = tenant.getId();

        FndModule module = new FndModule();
        module.setCode("TEST_MODULE_" + System.nanoTime());
        module.setDescription("Test Module");
        module.setActive(true);
        module = fndModuleRepository.save(module);

        Resource resource = new Resource();
        resource.setModule(module);
        resource.setCode("TEST_RESOURCE_" + System.nanoTime());
        resource.setDescription("Test Resource");
        resource.setActive(true);
        resource = resourceRepository.save(resource);
        String resourceCode = resource.getCode();
        Long resourceId = resource.getId();

        TenantContext.set(tenantId);
        Role role = new Role();
        role.setCode("PRIV_REFRESH_ROLE");
        role.setName("Priv Refresh Role");
        role.setSystem(false);
        role.setCreatedBy(0L);
        role.setCreatedAt(LocalDateTime.now());
        role = roleRepository.save(role);
        Long roleId = role.getId();
        TenantContext.clear();

        // Baseline: cache has no grant for this role yet (never refreshed for this brand-new role).
        privilegeCache.refresh();
        assertThat(privilegeCache.isGranted(tenantId, List.of("PRIV_REFRESH_ROLE"), resourceCode, Privilege.CREATE)).isFalse();

        // This call is @Transactional and internally calls privilegeCache.refresh() before
        // returning - simulating exactly what the RoleController /privileges endpoint does.
        TenantContext.set(tenantId);
        roleService.updatePrivileges(roleId, RolePrivilegeRq.builder()
                .grants(List.of(PrivilegeGrantRq.builder().resourceId(resourceId).privileges(List.of(Privilege.CREATE)).build()))
                .build());
        TenantContext.clear();

        // The whole point: this must be true WITHOUT calling privilegeCache.refresh() again and
        // WITHOUT a restart - updatePrivileges() itself is responsible for making its own change
        // visible in the cache once its transaction actually commits.
        boolean grantedRightAfterTheCall = privilegeCache.isGranted(tenantId, List.of("PRIV_REFRESH_ROLE"), resourceCode, Privilege.CREATE);
        assertThat(grantedRightAfterTheCall)
                .as("privilege granted via updatePrivileges() should be visible in the cache immediately, not only after a later refresh")
                .isTrue();
    }
}
