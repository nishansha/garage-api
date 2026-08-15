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
@Table(name = "inf_warehouse")
public class Warehouse extends GenericEntity {

    @Serial
    private static final long serialVersionUID = -3184756209481625730L;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "location")
    private String location;
}
