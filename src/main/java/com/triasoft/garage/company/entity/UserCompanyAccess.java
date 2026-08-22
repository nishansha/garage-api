package com.triasoft.garage.company.entity;

import com.triasoft.garage.entity.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;

/**
 * Grants a user access to a company (many rows per user — a bookkeeper working
 * across both companies gets one row per company). Never a hard 1:1 boundary;
 * see UserProfile.defaultCompanyId for the UX-only default.
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "user_company_access")
public class UserCompanyAccess extends TenantAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;
}
