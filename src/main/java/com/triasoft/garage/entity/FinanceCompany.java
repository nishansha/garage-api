package com.triasoft.garage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_finance_company")
public class FinanceCompany extends TenantAwareAuditEntity {

    @Serial
    private static final long serialVersionUID = 8213764592038475610L;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "email")
    private String email;
}
