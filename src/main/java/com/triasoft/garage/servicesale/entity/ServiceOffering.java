package com.triasoft.garage.servicesale.entity;

import com.triasoft.garage.entity.TenantAwareAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Catalog entry for a standalone service (e.g. car wash) — no product/inventory involved.
 * Named ServiceOffering, not Service, to avoid colliding with Spring's @Service stereotype
 * in every class that needs both.
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_service")
public class ServiceOffering extends TenantAwareAuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "default_rate", nullable = false)
    private BigDecimal defaultRate;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
