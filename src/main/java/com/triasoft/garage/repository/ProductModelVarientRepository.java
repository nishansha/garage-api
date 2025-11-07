package com.triasoft.garage.repository;

import com.triasoft.garage.entity.ProductModelVarient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductModelVarientRepository extends JpaRepository<ProductModelVarient,Long> {
    List<ProductModelVarient> findByProductBrandModelIdAndActiveTrue(Long modelId);
}
