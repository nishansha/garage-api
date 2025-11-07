package com.triasoft.garage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.util.List;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "fnd_brand_model")
public class ProductBrandModel extends GenericEntity {
    @Serial
    private static final long serialVersionUID = -2338857843855397396L;

    @ManyToOne
    @JoinColumn(name = "brand_id", nullable = false)
    private ProductBrand productBrand;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "active")
    private boolean active;

    @OneToMany(mappedBy = "productBrandModel", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductModelVarient> productModelVarients;
}
