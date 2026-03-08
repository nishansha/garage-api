package com.triasoft.garage.entity;

import com.triasoft.garage.constants.StatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_inventory")
@SoftDelete(columnName = "deleted")
public class Inventory extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = -4111029880972400132L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_detail_id", nullable = false)
    private PurchaseDetail purchaseOrderDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "uin", nullable = false)
    private String uin;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "landed_cost", nullable = false)
    private BigDecimal landedCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusEnum status;

    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;

    @Column(name = "product_no", nullable = false)
    private String productNo;

    @Column(name = "odometer")
    private Long odometer;

    @Column(name = "color")
    private String color;

    @Column(name = "source_sale_id")
    private Long sourceSaleId;

}
