package com.triasoft.garage;

import com.triasoft.garage.model.common.ApiResponse;
import com.triasoft.garage.model.tenant.TenantCreateRq;
import com.triasoft.garage.model.tenant.TenantCreateRs;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the tenant-provisioning endpoint going through the REAL servlet stack
 * (embedded Tomcat, real filter chain), unlike TenantProvisioningServiceTest (pure Mockito, no
 * Hibernate at all) or TenantIsolationIT (real DB, but calls services/repositories directly -
 * no HTTP layer). This class of bug - the created SUPERADMIN/ADMIN roles getting stamped with
 * tenant_id=-1 (the NO_TENANT sentinel) instead of the new tenant's id - only reproduced through
 * a real HTTP call, because it was caused by Spring's Open-Session-In-View pre-binding one
 * Hibernate session to the whole request thread (see spring.jpa.open-in-view: false in
 * application.yml for the full explanation); OSIV is only active behind a real servlet
 * container, so neither of the other two test styles could have caught it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantProvisioningHttpIT {

    private static final String DB_NAME = "garage_tenant_provisioning_http_it";

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
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String INTERNAL_API_KEY = "fxaSpRdDIIgFc7F91sEFfaIrFsBOw3B5LqK6dvR4qlo=";

    @Test
    void createTenant_viaRealHttpCall_rolesAreStampedWithTheNewTenantIdNotTheNoTenantSentinel() {
        TenantCreateRq rq = new TenantCreateRq();
        TenantCreateRq.TenantPart tenantPart = new TenantCreateRq.TenantPart();
        tenantPart.setCode("HTTP_IT_TENANT");
        tenantPart.setName("Http It Tenant");
        rq.setTenant(tenantPart);

        TenantCreateRq.SuperuserPart superuserPart = new TenantCreateRq.SuperuserPart();
        superuserPart.setUsername("http_it_owner");
        superuserPart.setPassword("Secret123!");
        superuserPart.setName("Http It Owner");
        superuserPart.setDesignation("Owner");
        rq.setSuperuser(superuserPart);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Api-Key", INTERNAL_API_KEY);

        ResponseEntity<ApiResponse<TenantCreateRs>> response = restTemplate.exchange(
                "/api/v1/internal/tenants",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(rq, headers),
                new org.springframework.core.ParameterizedTypeReference<ApiResponse<TenantCreateRs>>() {
                });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long tenantId = response.getBody().getData().getTenantId();
        assertThat(tenantId).isNotNull();

        Long roleTenantId = jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM fnd_role WHERE code = 'SUPERADMIN' AND tenant_id = ?",
                Long.class, tenantId);
        assertThat(roleTenantId).isEqualTo(tenantId);

        Integer roleCountForThisTenant = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fnd_role WHERE tenant_id = ?", Integer.class, tenantId);
        assertThat(roleCountForThisTenant).isEqualTo(2);

        String tenantStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM fnd_tenant WHERE id = ?", String.class, tenantId);
        assertThat(tenantStatus).isEqualTo("ACTIVE");
    }
}
