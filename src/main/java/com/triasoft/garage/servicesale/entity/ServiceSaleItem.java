package com.triasoft.garage.servicesale.entity;

import com.triasoft.garage.entity.TenantAwareAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_service_sale_item")
public class ServiceSaleItem extends TenantAwareAuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_sale_id", nullable = false)
    private ServiceSale serviceSale;

    // Nullable — a line can be ad-hoc (description only), not tied to the catalog.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_offering_id")
    private ServiceOffering serviceOffering;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "qty", nullable = false)
    private BigDecimal qty;

    @Column(name = "rate", nullable = false)
    private BigDecimal rate;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
}
