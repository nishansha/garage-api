package com.triasoft.garage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "fnd_model_varient")
public class ProductModelVarient extends GenericEntity{
    @Serial
    private static final long serialVersionUID = -2035932545060973788L;

    @ManyToOne
    @JoinColumn(name = "model_id", nullable = false)
    private ProductBrandModel productBrandModel;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "active")
    private boolean active;
}
