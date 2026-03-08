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
@Table(name = "app_customer")
public class Customer extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = -4853395816627727935L;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "mobile", nullable = false)
    private String mobile;

    @Column(name = "address")
    private String address;

    @Column(name = "email")
    private String email;

    @Column(name = "document_id")
    private Long documentId;
}
