package com.triasoft.garage.service;

import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.expense.ExpenseRs;
import com.triasoft.garage.model.expense.ExpenseSummaryRs;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseService {

    public ExpenseRs expenses(Pageable pageable, UserDTO user) {
        List<ExpenseDTO> expenses = new ArrayList<>();
        expenses.add(ExpenseDTO.builder()
                .id(1L)
                .date(LocalDate.now())
                .description("Office Rent for month of jan")
                .amount(new BigDecimal("12035"))
                .title("Office Rent")
                .build());
        expenses.add(ExpenseDTO.builder()
                .id(2L)
                .date(LocalDate.now())
                .description("Electricity Bill for Jan")
                .amount(new BigDecimal("1000"))
                .title("Electricity Bill")
                .build());
        ExpenseRs expenseRs = ExpenseRs.builder().expenses(expenses).build();
        expenseRs.setTotalPages(1);
        expenseRs.setTotalElements(2L);
        return expenseRs;
    }

    public ExpenseSummaryRs summary(UserDTO user) {
        return ExpenseSummaryRs.builder().companyExpenses("25,000").purchaseExpenses("1,25,000").purchaseExpThisMonth("1005").companyExpThisMonth("1400").build();
    }

    public ExpenseRs findExpenses(FilterRq filterRq, Pageable pageable, UserDTO user) {
        List<ExpenseDTO> expenses = new ArrayList<>();
        expenses.add(ExpenseDTO.builder()
                .id(1L)
                .date(LocalDate.now())
                .description("Office Rent for month of jan")
                .amount(new BigDecimal("12035"))
                .title("Office Rent")
                .build());
        ExpenseRs expenseRs = ExpenseRs.builder().expenses(expenses).build();
        expenseRs.setTotalPages(1);
        expenseRs.setTotalElements(1L);
        return expenseRs;
    }
}
