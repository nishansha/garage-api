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
@Table(name = "fnd_product_segment")
public class ProductSegment extends GenericEntity {
    @Serial
    private static final long serialVersionUID = 493615713953458593L;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory productCategory;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "active")
    private boolean active;
}
