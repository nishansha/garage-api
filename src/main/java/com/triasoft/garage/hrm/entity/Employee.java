package com.triasoft.garage.hrm.entity;

import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.TenantAwareAuditEntity;
import com.triasoft.garage.entity.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Payroll subject — distinct from UserProfile (a login/RBAC identity): not every employee
 * needs system access, and not every system user is on payroll. userProfile links the two
 * when both apply. */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_employee")
@SoftDelete(columnName = "deleted")
public class Employee extends TenantAwareAuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "employee_code", nullable = false)
    private String employeeCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "designation")
    private String designation;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "salary_amount", nullable = false)
    private BigDecimal salaryAmount;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_no")
    private String bankAccountNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id", nullable = false)
    private PaymentAccount paymentAccount;

    // EAGER, not LAZY — Hibernate 6 can't build a lazy proxy for an association whose
    // target entity is @SoftDelete (UserProfile) without bytecode enhancement, which this
    // project doesn't have configured.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
