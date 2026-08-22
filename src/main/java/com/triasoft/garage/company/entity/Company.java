package com.triasoft.garage.company.entity;

import com.triasoft.garage.entity.TenantAwareAuditEntity;
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
@Table(name = "app_company")
public class Company extends TenantAwareAuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "registration_no")
    private String registrationNo;

    @Column(name = "address")
    private String address;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
