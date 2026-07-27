package com.triasoft.garage.config;

import com.triasoft.garage.security.tenant.CurrentTenantResolver;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * Wires CurrentTenantResolver into Hibernate as a bean instance (not a class name), which is
 * what @TenantId-annotated entities use to resolve the tenant discriminator on every query.
 */
@Configuration
@RequiredArgsConstructor
public class HibernateTenantConfig implements HibernatePropertiesCustomizer {

    private final CurrentTenantResolver currentTenantResolver;

    @Override
    public void customize(java.util.Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantResolver);
    }
}
