package com.triasoft.garage.repository;

import com.triasoft.garage.entity.ProductBrandModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductBrandModelRepository extends JpaRepository<ProductBrandModel,Long> {
    List<ProductBrandModel> findByProductBrandIdAndActiveTrue(Long brandId);
}
