package com.triasoft.garage.hrm.service;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.hrm.dto.SalaryPaymentDTO;
import com.triasoft.garage.hrm.entity.Employee;
import com.triasoft.garage.hrm.entity.SalaryPayment;
import com.triasoft.garage.hrm.model.SalaryPaymentMarkPaidRq;
import com.triasoft.garage.hrm.model.SalaryPaymentRs;
import com.triasoft.garage.hrm.repository.EmployeeRepository;
import com.triasoft.garage.hrm.repository.SalaryPaymentRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.service.impl.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalaryPaymentService {

    private final SalaryPaymentRepository salaryPaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final JournalService journalService;

    public SalaryPaymentRs getAll(Long companyId) {
        List<SalaryPayment> payments = companyId != null
                ? salaryPaymentRepository.findByEmployeeCompanyId(companyId)
                : salaryPaymentRepository.findAll();
        return SalaryPaymentRs.builder().salaryPayments(payments.stream().map(this::toDTO).toList()).build();
    }

    /** Generates PENDING rows for every active employee in the given company who doesn't
     * already have one for this pay period — idempotent, safe to call more than once
     * (used by both SalaryRunScheduler and a manual "generate this month" trigger). */
    @Transactional
    public int generateForCompany(Long companyId, YearMonth period) {
        int created = 0;
        for (Employee employee : employeeRepository.findByCompanyIdAndActiveTrue(companyId)) {
            if (salaryPaymentRepository.existsByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(
                    employee.getId(), period.getMonthValue(), period.getYear())) {
                continue;
            }
            SalaryPayment payment = new SalaryPayment();
            payment.setEmployee(employee);
            payment.setPayPeriodMonth(period.getMonthValue());
            payment.setPayPeriodYear(period.getYear());
            payment.setGrossAmount(employee.getSalaryAmount());
            payment.setNetAmount(employee.getSalaryAmount());
            payment.setStatus(StatusEnum.PENDING);
            salaryPaymentRepository.save(payment);
            created++;
        }
        return created;
    }

    @Transactional
    public SalaryPaymentRs markPaid(Long id, SalaryPaymentMarkPaidRq rq) {
        SalaryPayment payment = findById(id);
        if (payment.getStatus() == StatusEnum.PAID) {
            throw new BusinessException(ErrorCode.Business.SALARY_PAYMENT_ALREADY_PAID);
        }
        PaymentAccount account = paymentAccountRepository.findById(rq.getPaymentAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));
        payment.setPaymentDate(rq.getPaymentDate());
        payment.setPaymentAccount(account);
        payment.setStatus(StatusEnum.PAID);
        salaryPaymentRepository.save(payment);

        journalService.post(JournalService.REF_SALARY_PAYMENT, id);
        return SalaryPaymentRs.builder().id(payment.getId()).build();
    }

    @Transactional
    public void delete(Long id) {
        SalaryPayment payment = findById(id);
        if (payment.getStatus() == StatusEnum.PAID) {
            journalService.reverseOnDate(JournalService.REF_SALARY_PAYMENT, id, LocalDate.now());
        }
        salaryPaymentRepository.delete(payment);
    }

    private SalaryPayment findById(Long id) {
        return salaryPaymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SALARY_PAYMENT_NOT_FOUND));
    }

    private SalaryPaymentDTO toDTO(SalaryPayment payment) {
        return SalaryPaymentDTO.builder()
                .id(payment.getId())
                .employeeId(payment.getEmployee().getId())
                .employeeName(payment.getEmployee().getName())
                .employeeCode(payment.getEmployee().getEmployeeCode())
                .payPeriodMonth(payment.getPayPeriodMonth())
                .payPeriodYear(payment.getPayPeriodYear())
                .grossAmount(payment.getGrossAmount())
                .netAmount(payment.getNetAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentAccountId(payment.getPaymentAccount() != null ? payment.getPaymentAccount().getId() : null)
                .paymentAccountName(payment.getPaymentAccount() != null ? payment.getPaymentAccount().getName() : null)
                .status(payment.getStatus().name())
                .notes(payment.getNotes())
                .build();
    }
}
