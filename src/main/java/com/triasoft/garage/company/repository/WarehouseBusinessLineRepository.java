package com.triasoft.garage.company.repository;

import com.triasoft.garage.company.constants.BusinessLine;
import com.triasoft.garage.company.entity.WarehouseBusinessLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseBusinessLineRepository extends JpaRepository<WarehouseBusinessLine, Long> {
    List<WarehouseBusinessLine> findByWarehouseId(Long warehouseId);

    boolean existsByWarehouseIdAndBusinessLine(Long warehouseId, BusinessLine businessLine);

    void deleteByWarehouseId(Long warehouseId);
}
