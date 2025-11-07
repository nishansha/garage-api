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
@Table(name = "fnd_product_brand")
public class ProductBrand extends GenericEntity {

    @Serial
    private static final long serialVersionUID = -6142391494558313532L;

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

    @OneToMany(mappedBy = "productBrand", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductBrandModel> brandModels;
}
