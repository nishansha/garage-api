package com.triasoft.garage.company.entity;

import com.triasoft.garage.company.constants.BusinessLine;
import com.triasoft.garage.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;

/** Which BusinessLine(s) a warehouse supports — a warehouse can have zero, one, or both rows. */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "warehouse_business_line")
public class WarehouseBusinessLine extends TenantAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_line", nullable = false)
    private BusinessLine businessLine;
}
