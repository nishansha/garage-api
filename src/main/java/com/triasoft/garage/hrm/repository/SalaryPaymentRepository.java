package com.triasoft.garage.hrm.repository;

import com.triasoft.garage.hrm.entity.SalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryPaymentRepository extends JpaRepository<SalaryPayment, Long> {
    List<SalaryPayment> findByEmployeeId(Long employeeId);

    List<SalaryPayment> findByEmployeeCompanyId(Long companyId);

    Optional<SalaryPayment> findByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(Long employeeId, int month, int year);

    boolean existsByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(Long employeeId, int month, int year);
}
