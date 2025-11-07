package com.triasoft.garage.repository;

import com.triasoft.garage.entity.ProductBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductBrandRepository extends JpaRepository<ProductBrand, Long> {
    List<ProductBrand> findByProductCategoryIdAndActiveTrue(Long categoryId);
}
