package com.triasoft.garage.hrm.repository;

import com.triasoft.garage.hrm.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByCompanyId(Long companyId);

    List<Employee> findByCompanyIdAndActiveTrue(Long companyId);

    boolean existsByCompanyIdAndEmployeeCodeIgnoreCase(Long companyId, String employeeCode);

    boolean existsByCompanyIdAndEmployeeCodeIgnoreCaseAndIdNot(Long companyId, String employeeCode, Long id);
}
