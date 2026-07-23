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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private ProductBrand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private ProductBrandModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "varient_id", nullable = false)
    private ProductModelVarient varient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private ProductSegment segment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fuel_type_id")
    private LookupMaster fuelType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transmission_type_id")
    private LookupMaster transmissionType;

    @Column(name = "document_id")
    private Long documentId;
}
