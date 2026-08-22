package com.triasoft.garage.servicesale.repository;

import com.triasoft.garage.servicesale.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {
    List<ServiceOffering> findByWarehouseId(Long warehouseId);

    boolean existsByWarehouseIdAndCodeIgnoreCase(Long warehouseId, String code);

    boolean existsByWarehouseIdAndCodeIgnoreCaseAndIdNot(Long warehouseId, String code, Long id);
}
