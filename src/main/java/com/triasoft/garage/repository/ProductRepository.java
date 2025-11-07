package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findBySku(String sku);
    List<Product> findByCategoryIdAndBrandIdAndModelIdAndVarientId(Long categoryId, Long brandId, Long modelId, Long varientId);
}
