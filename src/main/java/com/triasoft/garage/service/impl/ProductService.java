package com.triasoft.garage.service.impl;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductSegmentRepository productSegmentRepository;
    private final ProductBrandRepository productBrandRepository;
    private final ProductBrandModelRepository productBrandModelRepository;
    private final ProductModelVarientRepository productModelVarientRepository;

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

    public ProductRs getSegments(ProductRq productRq) {
        List<ProductSegment> segments = productSegmentRepository.findByProductCategoryIdAndActiveTrue(productRq.getCategoryId());
        if (CollectionUtils.isEmpty(segments))
            return ProductRs.builder().segments(List.of()).build();
        return ProductRs.builder().segments(segments.stream().map(this::toSegmentDTO).toList()).build();
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
        List<ProductModelVarient> varients = productModelVarientRepository.findByProductBrandModelIdAndActiveTrue(productRq.getModelId());
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

    private SegmentDTO toSegmentDTO(ProductSegment productSegment) {
        SegmentDTO segmentDTO = new SegmentDTO();
        BeanUtils.copyProperties(productSegment, segmentDTO);
        return segmentDTO;
    }

    private BrandDTO toBrandDTO(ProductBrand productBrand) {
        BrandDTO brandDTO = new BrandDTO();
        BeanUtils.copyProperties(productBrand, brandDTO);
        return brandDTO;
    }

    public Product createProduct(ProductRq productRq) {
        StringBuilder skuBuilder = new StringBuilder();
        StringBuilder productName = new StringBuilder();
        ProductBrand productBrand = productBrandRepository.findById(productRq.getBrandId()).orElse(null);
        if (Objects.isNull(productBrand))
            throw new BusinessException(ErrorCode.Business.PRD_BRAND_NOT_FOUND);
        ProductCategory productCategory = productBrand.getProductCategory();
        skuBuilder.append(productCategory.getCode()).append("-").append(productBrand.getCode());
        productName.append(productBrand.getDescription());
        ProductBrandModel productBrandModel = productBrandModelRepository.findById(productRq.getModelId()).orElse(null);
        if (Objects.nonNull(productBrandModel)) {
            skuBuilder.append("-").append(productBrandModel.getCode());
            productName.append(" / ").append(productBrandModel.getDescription());
        }
        ProductModelVarient productModelVarient = productModelVarientRepository.findById(productRq.getVarientId()).orElse(null);
        if (Objects.nonNull(productModelVarient)) {
            skuBuilder.append("-").append(productModelVarient.getCode());
            productName.append(" / ").append(productModelVarient.getDescription());
        }
        if (StringUtils.hasLength(productRq.getMakeYear())) {
            skuBuilder.append("-").append(productRq.getMakeYear());
            productName.append(" / ").append(productRq.getMakeYear());
        }
        Product newProduct = new Product();
        newProduct.setSku(skuBuilder.toString());
        newProduct.setName(StringUtils.hasLength(productRq.getName()) ? productRq.getName() : productName.toString());
        newProduct.setBrand(productBrand);
        newProduct.setModel(productBrandModel);
        newProduct.setVarient(productModelVarient);
        newProduct.setMakeYear(productRq.getMakeYear());
        newProduct.setCategory(productBrand.getProductCategory());
        return productRepository.save(newProduct);
    }

    @Transactional
    public ProductRs manageProductTypes(ProductRq productRq, UserDTO user) {
        switch (productRq.getType()) {
            case "CATEGORY":
                createOrUpdateCategory(productRq, user);
                break;
            case "SEGMENT":
                createOrUpdateSegment(productRq, user);
                break;
            case "BRAND":
                createOrUpdateBrand(productRq, user);
                break;
            case "MODEL":
                createOrUpdateModel(productRq, user);
                break;
            case "VARIANT":
                createOrUpdateVariant(productRq, user);
                break;
        }
        return ProductRs.builder().build();
    }

    private void createOrUpdateVariant(ProductRq productRq, UserDTO user) {
        ProductModelVarient variant = (productRq.getId() != null)
                ? productModelVarientRepository.findById(productRq.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_VARIANT_NOT_FOUND))
                : new ProductModelVarient();

        if (productRq.getId() == null) {
            if (productModelVarientRepository.findByProductBrandModelIdAndCodeIgnoreCase(productRq.getModelId(), productRq.getCode().trim()) != null) {
                throw new BusinessException(ErrorCode.Business.PRD_VARIENT_EXITS);
            }
            variant.setActive(true);
        }

        variant.setCode(productRq.getCode());
        variant.setDescription(productRq.getDescription());
        if (variant.getProductBrandModel() == null || !variant.getProductBrandModel().getId().equals(productRq.getModelId())) {
            variant.setProductBrandModel(productBrandModelRepository.findById(productRq.getModelId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_MODEL_NOT_FOUND)));
        }
        productModelVarientRepository.save(variant);
    }

    private void createOrUpdateModel(ProductRq productRq, UserDTO user) {
        ProductBrandModel productBrandModel = Objects.nonNull(productRq.getId()) ?
                productBrandModelRepository.findById(productRq.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_MODEL_NOT_FOUND))
                : new ProductBrandModel();

        if (Objects.isNull(productRq.getId())) {
            if (Objects.nonNull(productBrandModelRepository.findByProductBrandIdAndCodeIgnoreCase(productRq.getBrandId(), productRq.getCode().trim())))
                throw new BusinessException(ErrorCode.Business.PRD_MODEL_EXITS);
            productBrandModel.setActive(true);
        }
        productBrandModel.setCode(productRq.getCode());
        productBrandModel.setDescription(productRq.getDescription());
        if (productBrandModel.getProductBrand() == null || !productBrandModel.getProductBrand().getId().equals(productRq.getModelId())) {
            productBrandModel.setProductBrand(productBrandRepository.findById(productRq.getBrandId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_BRAND_NOT_FOUND)));
        }
        productBrandModelRepository.save(productBrandModel);
    }

    private void createOrUpdateBrand(ProductRq productRq, UserDTO user) {
        ProductBrand productBrand = Objects.nonNull(productRq.getId()) ? productBrandRepository.findById(productRq.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_BRAND_NOT_FOUND))
                : new ProductBrand();
        if (Objects.isNull(productRq.getId())) {
            if (Objects.nonNull(productBrandRepository.findByCodeIgnoreCase(productRq.getCode().trim())))
                throw new BusinessException(ErrorCode.Business.PRD_BRAND_EXITS);
            productBrand.setActive(true);
        }
        productBrand.setCode(productRq.getCode());
        productBrand.setDescription(productRq.getDescription());
        if (productBrand.getProductCategory() == null || !productBrand.getProductCategory().getId().equals(productRq.getCategoryId())) {
            productBrand.setProductCategory(productCategoryRepository.findById(productRq.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_CATEGORY_NOT_FOUND)));
        }
        productBrandRepository.save(productBrand);
    }

    private void createOrUpdateSegment(ProductRq productRq, UserDTO user) {
        ProductSegment segment = Objects.nonNull(productRq.getId()) ? productSegmentRepository.findById(productRq.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_SEGMENT_NOT_FOUND))
                : new ProductSegment();
        if (Objects.isNull(productRq.getId())) {
            if (Objects.nonNull(productSegmentRepository.findByCodeIgnoreCase(productRq.getCode().trim())))
                throw new BusinessException(ErrorCode.Business.PRD_SEGMENT_EXITS);
            segment.setActive(true);
        }
        segment.setCode(productRq.getCode());
        segment.setDescription(productRq.getDescription());
        if (segment.getProductCategory() == null || !segment.getProductCategory().getId().equals(productRq.getCategoryId())) {
            segment.setProductCategory(productCategoryRepository.findById(productRq.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_CATEGORY_NOT_FOUND)));
        }
        productSegmentRepository.save(segment);
    }

    private void createOrUpdateCategory(ProductRq productRq, UserDTO user) {
        ProductCategory category = Objects.nonNull(productRq.getId()) ? productCategoryRepository.findById(productRq.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_CATEGORY_NOT_FOUND))
                : new ProductCategory();
        if (Objects.isNull(productRq.getId())) {
            if (Objects.nonNull(productCategoryRepository.findByCodeIgnoreCase(productRq.getCode().trim())))
                throw new BusinessException(ErrorCode.Business.PRD_CATEGORY_EXISTS);
            category.setActive(true);
        }
        category.setCode(productRq.getCode());
        category.setDescription(productRq.getDescription());
        productCategoryRepository.save(category);
    }
}
