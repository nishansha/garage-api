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
@Table(name = "app_product")
public class Product extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = -5166801877656166200L;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne
    @JoinColumn(name = "brand_id", nullable = false)
    private ProductBrand brand;

    @ManyToOne
    @JoinColumn(name = "model_id", nullable = false)
    private ProductBrandModel model;

    @ManyToOne
    @JoinColumn(name = "varient_id", nullable = false)
    private ProductModelVarient varient;

    @Column(name = "document_id", nullable = false)
    private Long documentId;
}
