package com.triasoft.garage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "fnd_role")
@SoftDelete(columnName = "deleted")
public class Role extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    /**
     * System roles (e.g. SUPERADMIN) are seeded and can't be renamed/deleted via the admin UI.
     */
    @Column(name = "is_system", nullable = false)
    private boolean system;
}
