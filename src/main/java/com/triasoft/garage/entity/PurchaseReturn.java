package com.triasoft.garage.entity;

import com.triasoft.garage.constants.ReturnStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.RelationTargetAuditMode;
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
@Table(name = "app_purchase_return")
@SoftDelete(columnName = "deleted")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@AuditOverride(forClass = AuditGenericEntity.class)
public class PurchaseReturn extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_id", nullable = false, unique = true)
    private Inventory inventory;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(name = "reason")
    private String reason;

    @Column(name = "notes")
    private String notes;

    @Column(name = "inventory_landed_cost", nullable = false)
    private BigDecimal inventoryLandedCost;

    @Column(name = "return_amount", nullable = false)
    private BigDecimal returnAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnStatusEnum status = ReturnStatusEnum.PENDING;

    @OneToMany(mappedBy = "purchaseReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<PurchaseReturnReceipt> receipts = new ArrayList<>();
}
