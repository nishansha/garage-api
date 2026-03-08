package com.triasoft.garage.repository;

import com.triasoft.garage.entity.ProductSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSegmentRepository extends JpaRepository<ProductSegment, Long> {
    List<ProductSegment> findByProductCategoryIdAndActiveTrue(Long categoryId);
    ProductSegment findByCodeIgnoreCase(String trim);
}
