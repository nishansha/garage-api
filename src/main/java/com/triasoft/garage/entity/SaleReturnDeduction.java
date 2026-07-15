package com.triasoft.garage.entity;

import com.triasoft.garage.constants.DeductionContextEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_sale_return_deduction")
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@AuditOverride(forClass = AuditGenericEntity.class)
public class SaleReturnDeduction extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "sale_return_id", nullable = false)
    private SaleReturn saleReturn;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_context", nullable = false)
    private DeductionContextEnum vehicleContext;

    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
}
