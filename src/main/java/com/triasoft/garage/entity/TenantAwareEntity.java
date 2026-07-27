package com.triasoft.garage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.io.Serial;

@Getter
@Setter
@MappedSuperclass
public class TenantAwareEntity extends GenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;
}
