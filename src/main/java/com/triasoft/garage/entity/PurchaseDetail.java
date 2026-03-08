package com.triasoft.garage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_purchase_order_detail")
public class PurchaseDetail extends GenericEntity {

    @Serial
    private static final long serialVersionUID = 8487354604294485228L;

    @Column(name = "item_uid", nullable = false)
    private String uuid;

    @Column(name = "ownership_serial_no")
    private String ownershipSerialNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private Purchase purchase;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "discount")
    private Double discount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "tax")
    private Double tax;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Column(name = "product_no")
    private String productNo;

    @Column(name = "odometer")
    private String odometer;

}
