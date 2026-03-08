package com.triasoft.garage.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_product")
@SoftDelete(columnName = "deleted")
public class Product extends AuditGenericEntity {

    @Serial
    private static final long serialVersionUID = -5166801877656166200L;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
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

    @Column(name = "make_year")
    private String makeYear;

    @Column(name = "document_id")
    private Long documentId;
}
