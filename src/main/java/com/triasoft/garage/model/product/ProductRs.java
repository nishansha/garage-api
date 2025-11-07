package com.triasoft.garage.model.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.triasoft.garage.dto.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductRs implements Serializable {
    @Serial
    private static final long serialVersionUID = 2650363292789132011L;
    private ProductDTO product;
    private List<ProductDTO> products;

    private CategoryDTO category;
    private List<CategoryDTO> categories;

    private BrandDTO brand;
    private List<BrandDTO> brands;

    private ModelDTO model;
    private List<ModelDTO> models;

    private VarientDTO varient;
    private List<VarientDTO> varients;

}
