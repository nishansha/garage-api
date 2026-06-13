package com.triasoft.garage.entity;

import com.triasoft.garage.constants.AccountTypeEnum;
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
@Table(name = "app_payment_account")
public class PaymentAccount extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 4712938461029384756L;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_no")
    private String accountNo;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountTypeEnum accountType;

    @Column(name = "opening_balance", nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coa_id")
    private ChartOfAccount chartOfAccount;

}
