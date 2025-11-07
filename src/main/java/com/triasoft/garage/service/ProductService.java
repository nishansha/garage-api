package com.triasoft.garage.service;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.*;
import com.triasoft.garage.entity.*;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.product.ProductRq;
import com.triasoft.garage.model.product.ProductRs;
import com.triasoft.garage.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductBrandRepository productBrandRepository;
    private final ProductBrandModelRepository productBrandModelRepository;
    private final ProductModelVarientRepository ProductModelVarientRepository;

    public ProductRs getProducts(ProductRq productRq) {
        List<Product> products = productRepository.findAll();
        if (CollectionUtils.isEmpty(products))
            return ProductRs.builder().products(List.of()).build();
        return ProductRs.builder().products(products.stream().map(this::toProductDTO).toList()).build();
    }

    public ProductRs getProduct(ProductRq productRq) {
        Product product = productRepository.findById(productRq.getId()).orElse(null);
        if (Objects.isNull(product))
            throw new BusinessException(ErrorCode.Business.PRD_NOT_FOUND);
        return ProductRs.builder().product(toProductDTO(product)).build();
    }

    public ProductRs getFilteredProducts(ProductRq productRq) {
        List<Product> products = productRepository.findByCategoryIdAndBrandIdAndModelIdAndVarientId(productRq.getCategoryId(), productRq.getBrandId(), productRq.getModelId(), productRq.getVarientId());
        if (CollectionUtils.isEmpty(products))
            return ProductRs.builder().products(List.of()).build();
        return ProductRs.builder().products(products.stream().map(this::toProductDTO).toList()).build();
    }

    private ProductDTO toProductDTO(Product product) {
        ProductDTO productDTO = new ProductDTO();
        BeanUtils.copyProperties(product, productDTO);
        return productDTO;
    }

    public ProductRs getCategories(ProductRq productRq) {
        List<ProductCategory> categories = productCategoryRepository.findByActiveTrue();
        if (CollectionUtils.isEmpty(categories))
            return ProductRs.builder().categories(List.of()).build();
        return ProductRs.builder().categories(categories.stream().map(this::toCategoryDTO).toList()).build();
    }

    public ProductRs getBrands(ProductRq productRq) {
        List<ProductBrand> brands = productBrandRepository.findByProductCategoryIdAndActiveTrue(productRq.getCategoryId());
        if (CollectionUtils.isEmpty(brands))
            return ProductRs.builder().brands(List.of()).build();
        return ProductRs.builder().brands(brands.stream().map(this::toBrandDTO).toList()).build();
    }

    public ProductRs getModels(ProductRq productRq) {
        List<ProductBrandModel> models = productBrandModelRepository.findByProductBrandIdAndActiveTrue(productRq.getBrandId());
        if (CollectionUtils.isEmpty(models))
            return ProductRs.builder().models(List.of()).build();
        return ProductRs.builder().models(models.stream().map(this::toModelDTO).toList()).build();
    }

    public ProductRs getVarients(ProductRq productRq) {
        List<ProductModelVarient> varients = ProductModelVarientRepository.findByProductBrandModelIdAndActiveTrue(productRq.getModelId());
        if (CollectionUtils.isEmpty(varients))
            return ProductRs.builder().varients(List.of()).build();
        return ProductRs.builder().varients(varients.stream().map(this::toVarientDTO).toList()).build();
    }

    private VarientDTO toVarientDTO(ProductModelVarient productModelVarient) {
        VarientDTO varientDTO = new VarientDTO();
        BeanUtils.copyProperties(productModelVarient, varientDTO);
        return varientDTO;
    }

    private ModelDTO toModelDTO(ProductBrandModel productBrandModel) {
        ModelDTO modelDTO = new ModelDTO();
        BeanUtils.copyProperties(productBrandModel, modelDTO);
        return modelDTO;
    }

    private CategoryDTO toCategoryDTO(ProductCategory productCategory) {
        CategoryDTO categoryDTO = new CategoryDTO();
        BeanUtils.copyProperties(productCategory, categoryDTO);
        return categoryDTO;
    }

    private BrandDTO toBrandDTO(ProductBrand productBrand) {
        BrandDTO brandDTO = new BrandDTO();
        BeanUtils.copyProperties(productBrand, brandDTO);
        return brandDTO;
    }
}
