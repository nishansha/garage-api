package com.triasoft.garage.hrm.service;

import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.hrm.entity.Employee;
import com.triasoft.garage.hrm.entity.SalaryPayment;
import com.triasoft.garage.hrm.model.SalaryPaymentMarkPaidRq;
import com.triasoft.garage.hrm.repository.EmployeeRepository;
import com.triasoft.garage.hrm.repository.SalaryPaymentRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.service.impl.JournalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the accrual-posting orchestration added to SalaryPaymentService: generateForCompany
 * now posts a SALARY_ACCRUAL journal alongside every new PENDING row (see JournalService.
 * handleSalaryAccrual), and delete() must reverse that accrual regardless of status, plus the
 * settlement (REF_SALARY_PAYMENT) only if the row had already been marked PAID — same
 * settlement-then-accrual ordering as PurchaseService.deleteInternal.
 */
@ExtendWith(MockitoExtension.class)
class SalaryPaymentServiceTest {

    @Mock private SalaryPaymentRepository salaryPaymentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PaymentAccountRepository paymentAccountRepository;
    @Mock private JournalService journalService;

    private SalaryPaymentService salaryPaymentService;

    @BeforeEach
    void setUp() {
        salaryPaymentService = new SalaryPaymentService(
                salaryPaymentRepository, employeeRepository, paymentAccountRepository, journalService);
    }

    private Employee buildEmployee() {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setCompanyId(1L);
        employee.setName("Jane Doe");
        employee.setEmployeeCode("EMP-1");
        employee.setSalaryAmount(new BigDecimal("50000"));
        return employee;
    }

    @Test
    void generateForCompany_newEmployee_savesPendingRowAndPostsAccrual() {
        Employee employee = buildEmployee();
        when(employeeRepository.findByCompanyIdAndActiveTrue(1L)).thenReturn(List.of(employee));
        when(salaryPaymentRepository.existsByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(1L, 7, 2026))
                .thenReturn(false);
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenAnswer(inv -> {
            SalaryPayment payment = inv.getArgument(0);
            payment.setId(10L);
            return payment;
        });

        int created = salaryPaymentService.generateForCompany(1L, YearMonth.of(2026, 7));

        org.assertj.core.api.Assertions.assertThat(created).isEqualTo(1);
        verify(salaryPaymentRepository).save(any(SalaryPayment.class));
        verify(journalService).post(JournalService.REF_SALARY_ACCRUAL, 10L);
    }

    @Test
    void generateForCompany_alreadyGeneratedForPeriod_skipsSaveAndAccrual() {
        Employee employee = buildEmployee();
        when(employeeRepository.findByCompanyIdAndActiveTrue(1L)).thenReturn(List.of(employee));
        when(salaryPaymentRepository.existsByEmployeeIdAndPayPeriodMonthAndPayPeriodYear(1L, 7, 2026))
                .thenReturn(true);

        int created = salaryPaymentService.generateForCompany(1L, YearMonth.of(2026, 7));

        org.assertj.core.api.Assertions.assertThat(created).isZero();
        verify(salaryPaymentRepository, never()).save(any());
        verify(journalService, never()).post(eq(JournalService.REF_SALARY_ACCRUAL), any());
    }

    private SalaryPayment buildSalaryPayment(StatusEnum status) {
        SalaryPayment payment = new SalaryPayment();
        payment.setId(5L);
        payment.setEmployee(buildEmployee());
        payment.setPayPeriodMonth(7);
        payment.setPayPeriodYear(2026);
        payment.setGrossAmount(new BigDecimal("50000"));
        payment.setNetAmount(new BigDecimal("50000"));
        payment.setStatus(status);
        return payment;
    }

    @Test
    void delete_pendingRow_reversesAccrualOnly() {
        when(salaryPaymentRepository.findById(5L)).thenReturn(Optional.of(buildSalaryPayment(StatusEnum.PENDING)));

        salaryPaymentService.delete(5L);

        verify(journalService).reverse(JournalService.REF_SALARY_ACCRUAL, 5L);
        verify(journalService, never()).reverseOnDate(eq(JournalService.REF_SALARY_PAYMENT), any(), any());
        verify(salaryPaymentRepository).delete(any(SalaryPayment.class));
    }

    @Test
    void delete_paidRow_reversesSettlementBeforeAccrual() {
        when(salaryPaymentRepository.findById(5L)).thenReturn(Optional.of(buildSalaryPayment(StatusEnum.PAID)));

        salaryPaymentService.delete(5L);

        InOrder order = inOrder(journalService);
        order.verify(journalService).reverseOnDate(eq(JournalService.REF_SALARY_PAYMENT), eq(5L), any(LocalDate.class));
        order.verify(journalService).reverse(JournalService.REF_SALARY_ACCRUAL, 5L);
        verify(salaryPaymentRepository).delete(any(SalaryPayment.class));
    }

    @Test
    void markPaid_postsSettlementJournal() {
        SalaryPayment payment = buildSalaryPayment(StatusEnum.PENDING);
        when(salaryPaymentRepository.findById(5L)).thenReturn(Optional.of(payment));
        PaymentAccount account = new PaymentAccount();
        account.setId(2L);
        when(paymentAccountRepository.findById(2L)).thenReturn(Optional.of(account));

        SalaryPaymentMarkPaidRq rq = new SalaryPaymentMarkPaidRq();
        rq.setPaymentAccountId(2L);
        rq.setPaymentDate(LocalDate.of(2026, 8, 5));

        salaryPaymentService.markPaid(5L, rq);

        verify(journalService).post(JournalService.REF_SALARY_PAYMENT, 5L);
    }
}
