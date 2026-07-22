package com.triasoft.garage.entity;

import com.triasoft.garage.constants.Privilege;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "fnd_role_privilege")
public class RolePrivilege extends GenericEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "privilege", nullable = false)
    private Privilege privilege;
}
