package com.triasoft.garage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_purchase_order")
@SoftDelete(columnName = "deleted")
public class Purchase extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = -7193141372297102034L;

    @Column(name = "reference_no", unique = true, nullable = false)
    private String referenceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "delivered_date")
    private LocalDate deliveredDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private LookupMaster status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "notes")
    private String notes;

    @Column(name = "pickup_staff_id")
    private Long pickupStaffId;

    @Column(name = "pickup_location")
    private String pickupLocation;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseDetail> purchaseDetails = new ArrayList<>();

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> purchaseExpenses = new ArrayList<>();

}
