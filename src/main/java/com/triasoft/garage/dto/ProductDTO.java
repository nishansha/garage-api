package com.triasoft.garage.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ProductDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -2369130483152407416L;
    private String sku;
    private String name;
    private String description;
    private CategoryDTO category;
    private BrandDTO brand;
    private ModelDTO model;
    private VarientDTO varient;
    private Long documentId;
}
