package com.triasoft.garage.entity;

import com.triasoft.garage.constants.ExpenseLockWindow;
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
@Table(name = "fnd_product_category")
public class ProductCategory extends GenericEntity {

    @Serial
    private static final long serialVersionUID = 5845814128166711539L;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "active")
    private boolean active;

    @Column(name = "expense_lock_enabled", nullable = false)
    private boolean expenseLockEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_lock_window")
    private ExpenseLockWindow expenseLockWindow;

    @OneToMany(mappedBy = "productCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductBrand> productBrands;
}
