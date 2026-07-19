package com.triasoft.garage.repository;

import com.triasoft.garage.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findBySku(String sku);
    List<Product> findByCategoryIdAndBrandIdAndModelIdAndVarientId(Long categoryId, Long brandId, Long modelId, Long varientId);

    /**
     * Finds an existing product to reuse for a purchase. A product's identity is
     * brand + model + variant + fuel type + transmission type; a null fuel/transmission
     * on the request only matches a product that also has it null.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.brand.id = :brandId
              AND p.model.id = :modelId
              AND p.varient.id = :variantId
              AND ((CAST(:fuelTypeId AS long) IS NULL AND p.fuelType IS NULL) OR p.fuelType.id = :fuelTypeId)
              AND ((CAST(:transmissionTypeId AS long) IS NULL AND p.transmissionType IS NULL) OR p.transmissionType.id = :transmissionTypeId)
            """)
    Optional<Product> findForReuse(@Param("brandId") Long brandId,
                                   @Param("modelId") Long modelId,
                                   @Param("variantId") Long variantId,
                                   @Param("fuelTypeId") Long fuelTypeId,
                                   @Param("transmissionTypeId") Long transmissionTypeId);
}
