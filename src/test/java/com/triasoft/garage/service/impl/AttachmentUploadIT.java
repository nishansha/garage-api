package com.triasoft.garage.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.triasoft.garage.config.AppProperties;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.Attachment;
import com.triasoft.garage.entity.Tenant;
import com.triasoft.garage.model.common.AttachmentRs;
import com.triasoft.garage.model.common.UploadRs;
import com.triasoft.garage.repository.AttachmentRepository;
import com.triasoft.garage.repository.TenantRepository;
import com.triasoft.garage.security.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check of the generic Attachment/Upload module against a real (throwaway) Postgres
 * database and the real local MinIO instance - same rationale/shape as {@code TenantIsolationIT}:
 * exercises the actual {@code app_attachment} DDL, the {@code @TenantId} scoping on the new
 * entity, and a real MinIO put/get round trip, rather than mocking any of it.
 * <p>
 * Named {@code *IT} so default {@code mvn test} skips it. Run explicitly with
 * {@code mvn test -Dtest=AttachmentUploadIT}. Requires a local Postgres reachable the same way
 * {@code application-dev.yml} expects, and a local MinIO reachable at the {@code minio} profile's
 * configured endpoint/credentials.
 */
@SpringBootTest
class AttachmentUploadIT {

    private static final String DB_NAME = "garage_attachment_it";

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
    private UploadService uploadService;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private AppProperties appProperties;

    @BeforeEach
    void ensureBucketExists() throws Exception {
        String bucket = appProperties.getStorage().getMinio().getBucket();
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Long createTenant(String code) {
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(code);
        tenant.setStatus("ACTIVE");
        tenant.setCreatedBy(0L);
        tenant.setCreatedAt(java.time.LocalDateTime.now());
        return tenantRepository.save(tenant).getId();
    }

    private void authenticateAs(Long userId, Long tenantId) throws Exception {
        UserDTO userDTO = UserDTO.builder().id(userId).tenantId(tenantId).name("Test User").build();
        String userJson = new ObjectMapper().writeValueAsString(userDTO);
        Claims claims = Jwts.claims().add("user", userJson).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(claims, null, List.of()));
    }

    @Test
    void uploadPersistsOneAttachmentPerFileWithDistinctKeysAndOwnContentType() throws Exception {
        Long tenantId = createTenant("ATTACH_TENANT_A");
        TenantContext.set(tenantId);
        authenticateAs(1L, tenantId);

        MockMultipartFile photo = new MockMultipartFile("files", "car-front.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile doc = new MockMultipartFile("files", "rc-book.pdf", "application/pdf", "fake-pdf-bytes".getBytes(StandardCharsets.UTF_8));

        UploadRs response = uploadService.upload("INVENTORY", 51L, "PHOTO", List.of(photo, doc), null);

        assertThat(response.getAttachments()).hasSize(2);
        assertThat(response.getAttachments()).extracting(AttachmentRs::getFileName)
                .containsExactlyInAnyOrder("car-front.jpg", "rc-book.pdf");
        assertThat(response.getAttachments()).extracting(AttachmentRs::getContentType)
                .containsExactlyInAnyOrder("image/jpeg", "application/pdf");

        List<Attachment> persisted = attachmentRepository.findByEntityTypeAndEntityId("INVENTORY", 51L);
        assertThat(persisted).hasSize(2);
        assertThat(persisted).extracting(Attachment::getObjectKey).doesNotHaveDuplicates();
        assertThat(persisted).allMatch(a -> a.getTenantId().equals(tenantId));
    }

    @Test
    void listFiltersByEntityAndCategory() throws Exception {
        Long tenantId = createTenant("ATTACH_TENANT_B");
        TenantContext.set(tenantId);
        authenticateAs(2L, tenantId);

        MockMultipartFile photo = new MockMultipartFile("files", "car-side.jpg", "image/jpeg", "fake-jpeg-bytes-2".getBytes(StandardCharsets.UTF_8));
        uploadService.upload("INVENTORY", 52L, "PHOTO", List.of(photo), null);
        MockMultipartFile idProof = new MockMultipartFile("files", "vendor-id.pdf", "application/pdf", "fake-id-bytes".getBytes(StandardCharsets.UTF_8));
        uploadService.upload("VENDOR", 7L, "ID_PROOF", List.of(idProof), null);

        List<AttachmentRs> inventoryPhotos = uploadService.list("INVENTORY", 52L, null);
        assertThat(inventoryPhotos).hasSize(1);
        assertThat(inventoryPhotos.get(0).getFileName()).isEqualTo("car-side.jpg");

        List<AttachmentRs> vendorDocs = uploadService.list("VENDOR", 7L, "ID_PROOF");
        assertThat(vendorDocs).hasSize(1);
        assertThat(vendorDocs.get(0).getFileName()).isEqualTo("vendor-id.pdf");

        assertThat(uploadService.list("INVENTORY", 999L, null)).isEmpty();
    }

    @Test
    void downloadRoundTripsTheOriginalBytesAndMetadata() throws Exception {
        Long tenantId = createTenant("ATTACH_TENANT_C");
        TenantContext.set(tenantId);
        authenticateAs(3L, tenantId);

        byte[] originalBytes = "the-real-car-photo-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile photo = new MockMultipartFile("files", "car-rear.jpg", "image/jpeg", originalBytes);
        UploadRs uploadRs = uploadService.upload("INVENTORY", 51L, "PHOTO", List.of(photo), null);
        Long attachmentId = uploadRs.getAttachments().get(0).getId();

        UploadService.DownloadedFile downloaded = uploadService.download(attachmentId);

        assertThat(downloaded.contentType()).isEqualTo("image/jpeg");
        assertThat(downloaded.fileName()).isEqualTo("car-rear.jpg");
        try (InputStream stream = downloaded.stream()) {
            assertThat(stream.readAllBytes()).isEqualTo(originalBytes);
        }
    }

    @Test
    void attachmentsAreIsolatedPerTenant() throws Exception {
        Long tenantA = createTenant("ATTACH_TENANT_D");
        Long tenantB = createTenant("ATTACH_TENANT_E");

        TenantContext.set(tenantA);
        authenticateAs(4L, tenantA);
        uploadService.upload("INVENTORY", 60L, "PHOTO", List.of(new MockMultipartFile("files", "a.jpg", "image/jpeg", "a".getBytes(StandardCharsets.UTF_8))), null);
        TenantContext.clear();
        SecurityContextHolder.clearContext();

        TenantContext.set(tenantB);
        authenticateAs(5L, tenantB);
        uploadService.upload("INVENTORY", 60L, "PHOTO", List.of(new MockMultipartFile("files", "b.jpg", "image/jpeg", "b".getBytes(StandardCharsets.UTF_8))), null);

        List<AttachmentRs> seenByB = uploadService.list("INVENTORY", 60L, null);
        assertThat(seenByB).extracting(AttachmentRs::getFileName).containsExactly("b.jpg");
    }
}
