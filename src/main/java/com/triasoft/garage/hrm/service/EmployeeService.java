package com.triasoft.garage.hrm.service;

import com.triasoft.garage.company.repository.CompanyRepository;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.UserProfile;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.hrm.dto.EmployeeDTO;
import com.triasoft.garage.hrm.entity.Employee;
import com.triasoft.garage.hrm.model.EmployeeRq;
import com.triasoft.garage.hrm.model.EmployeeRs;
import com.triasoft.garage.hrm.repository.EmployeeRepository;
import com.triasoft.garage.locking.VersionCheck;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final UserProfileRepository userProfileRepository;

    public EmployeeRs getAll(Long companyId) {
        List<Employee> employees = companyId != null
                ? employeeRepository.findByCompanyId(companyId)
                : employeeRepository.findAll();
        return EmployeeRs.builder().employees(employees.stream().map(this::toDTO).toList()).build();
    }

    public EmployeeDTO get(Long id) {
        return toDTO(findById(id));
    }

    @Transactional
    public EmployeeRs create(EmployeeRq rq) {
        if (!companyRepository.existsById(rq.getCompanyId())) {
            throw new BusinessException(ErrorCode.Business.COMPANY_NOT_FOUND);
        }
        String code = rq.getEmployeeCode().trim().toUpperCase();
        if (employeeRepository.existsByCompanyIdAndEmployeeCodeIgnoreCase(rq.getCompanyId(), code)) {
            throw new BusinessException(ErrorCode.Business.EMPLOYEE_CODE_EXISTS);
        }
        Employee employee = new Employee();
        employee.setCompanyId(rq.getCompanyId());
        employee.setEmployeeCode(code);
        applyFields(employee, rq);
        employeeRepository.save(employee);
        return EmployeeRs.builder().id(employee.getId()).build();
    }

    @Transactional
    @VersionCheck(entity = Employee.class)
    public EmployeeRs update(Long id, EmployeeRq rq) {
        Employee employee = findById(id);
        String code = rq.getEmployeeCode().trim().toUpperCase();
        if (employeeRepository.existsByCompanyIdAndEmployeeCodeIgnoreCaseAndIdNot(employee.getCompanyId(), code, id)) {
            throw new BusinessException(ErrorCode.Business.EMPLOYEE_CODE_EXISTS);
        }
        employee.setEmployeeCode(code);
        applyFields(employee, rq);
        employeeRepository.save(employee);
        return EmployeeRs.builder().id(employee.getId()).build();
    }

    @Transactional
    public void delete(Long id) {
        employeeRepository.delete(findById(id));
    }

    private void applyFields(Employee employee, EmployeeRq rq) {
        employee.setName(rq.getName());
        employee.setDesignation(rq.getDesignation());
        employee.setJoinDate(rq.getJoinDate());
        employee.setTerminationDate(rq.getTerminationDate());
        employee.setSalaryAmount(rq.getSalaryAmount());
        employee.setBankName(rq.getBankName());
        employee.setBankAccountNo(rq.getBankAccountNo());
        PaymentAccount account = paymentAccountRepository.findById(rq.getPaymentAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));
        employee.setPaymentAccount(account);
        if (rq.getUserProfileId() != null) {
            UserProfile userProfile = userProfileRepository.findById(rq.getUserProfileId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.USER_NOT_FOUND));
            employee.setUserProfile(userProfile);
        } else {
            employee.setUserProfile(null);
        }
        employee.setActive(rq.isActive());
    }

    private Employee findById(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.EMPLOYEE_NOT_FOUND));
    }

    private EmployeeDTO toDTO(Employee employee) {
        return EmployeeDTO.builder()
                .id(employee.getId())
                .version(employee.getVersion())
                .companyId(employee.getCompanyId())
                .employeeCode(employee.getEmployeeCode())
                .name(employee.getName())
                .designation(employee.getDesignation())
                .joinDate(employee.getJoinDate())
                .terminationDate(employee.getTerminationDate())
                .salaryAmount(employee.getSalaryAmount())
                .bankName(employee.getBankName())
                .bankAccountNo(employee.getBankAccountNo())
                .paymentAccountId(employee.getPaymentAccount().getId())
                .paymentAccountName(employee.getPaymentAccount().getName())
                .userProfileId(employee.getUserProfile() != null ? employee.getUserProfile().getId() : null)
                .active(employee.isActive())
                .build();
    }
}
