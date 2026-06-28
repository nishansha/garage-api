package com.triasoft.garage.repository;

import com.triasoft.garage.entity.SaleReturnDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleReturnDeductionRepository extends JpaRepository<SaleReturnDeduction, Long> {
    List<SaleReturnDeduction> findBySaleReturnId(Long saleReturnId);
}
