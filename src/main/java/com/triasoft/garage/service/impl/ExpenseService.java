package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.Expense;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.expense.ExpenseRq;
import com.triasoft.garage.model.expense.ExpenseRs;
import com.triasoft.garage.model.expense.ExpenseSummaryRs;
import com.triasoft.garage.projection.ExpenseMetrics;
import com.triasoft.garage.repository.ExpenseRepository;
import com.triasoft.garage.specifiction.ExpenseSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final AccountService accountService;

    public ExpenseRs getAll(Pageable pageable, String type, UserDTO user) {
        Page<Expense> expensePage;
        if("P".equalsIgnoreCase(type)){
            expensePage = expenseRepository.findByPurchaseIsNotNull(pageable);
        }else{
            expensePage = expenseRepository.findByPurchaseIsNull(pageable);
        }
        List<ExpenseDTO> expenses = expensePage.getContent().stream().map(this::convertToDTO).toList();
        ExpenseRs expenseRs = ExpenseRs.builder().expenses(expenses).build();
        expenseRs.setTotalPages(expensePage.getTotalPages());
        expenseRs.setTotalElements(expensePage.getTotalElements());
        return expenseRs;
    }

    public ExpenseSummaryRs summary(UserDTO user) {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        ExpenseMetrics metrics = expenseRepository.getExpenseMetrics(startOfMonth);
        return ExpenseSummaryRs.builder()
                .companyExpenses((metrics.getTotalGeneralExpense() != null ? metrics.getTotalGeneralExpense() : BigDecimal.ZERO).toString())
                .purchaseExpenses((metrics.getTotalPurchaseExpense() != null ? metrics.getTotalPurchaseExpense() : BigDecimal.ZERO).toString())
                .companyExpThisMonth((metrics.getGeneralExpenseThisMonth() != null ? metrics.getGeneralExpenseThisMonth() : BigDecimal.ZERO).toString())
                .purchaseExpThisMonth((metrics.getPurchaseExpenseThisMonth() != null ? metrics.getPurchaseExpenseThisMonth() : BigDecimal.ZERO).toString())
                .build();
    }

    public ExpenseRs search(FilterRq filterRq,String type, Pageable pageable, UserDTO user) {
        Specification<Expense> spec = ExpenseSpecification.buildGeneralSearchQuery(filterRq, type);
        Page<Expense> expensePage = expenseRepository.findAll(spec, pageable);
        List<ExpenseDTO> expenses = expensePage.getContent().stream()
                .map(this::convertToDTO)
                .toList();
        ExpenseRs expenseRs = ExpenseRs.builder().expenses(expenses).build();
        expenseRs.setTotalPages(expensePage.getTotalPages());
        expenseRs.setTotalElements(expensePage.getTotalElements());
        return expenseRs;
    }

    public ExpenseRs create(ExpenseRq expenseRq, UserDTO user) {
        Expense expense = new Expense();
        expense.setDate(expenseRq.getDate());
        expense.setAmount(expenseRq.getAmount());
        expense.setDescription(expenseRq.getDescription());
        ExpenseDTO exDto = new ExpenseDTO();
        BeanUtils.copyProperties(expenseRq, exDto);
        expense.setExpenseAccount(accountService.getOrCreateExpenseAccount(exDto, user));
        expenseRepository.save(expense);
        return ExpenseRs.builder().build();
    }

    public ExpenseDTO get(Long id, UserDTO user) {
        Expense expense = expenseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));
        return convertToDTO(expense);
    }

    public ExpenseRs update(Long id, ExpenseRq expenseRq, UserDTO user) {
        Expense expense = expenseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));
        expense.setDate(expenseRq.getDate());
        expense.setAmount(expenseRq.getAmount());
        expense.setDescription(expenseRq.getDescription());
        Long currentAccountId = (expense.getExpenseAccount() != null) ? expense.getExpenseAccount().getId() : null;
        if (!Objects.equals(currentAccountId, expenseRq.getTypeId())) {
            ExpenseDTO exDto = new ExpenseDTO();
            BeanUtils.copyProperties(expenseRq, exDto);
            expense.setExpenseAccount(accountService.getOrCreateExpenseAccount(exDto, user));
        }
        expenseRepository.save(expense);
        return ExpenseRs.builder().build();
    }

    public ExpenseRs delete(Long id, UserDTO user) {
        Expense expense = expenseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));
        expenseRepository.delete(expense);
        return ExpenseRs.builder().build();
    }

    private ExpenseDTO convertToDTO(Expense expense) {
        return ExpenseDTO.builder()
                .id(expense.getId())
                .date(expense.getDate())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .title(Objects.nonNull(expense.getExpenseAccount()) ? expense.getExpenseAccount().getName() : null)
                .typeId(Objects.nonNull(expense.getExpenseAccount()) ? expense.getExpenseAccount().getId() : null)
                .build();
    }
}
