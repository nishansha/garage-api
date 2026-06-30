package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.ExpenseLockWindow;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.constants.TransactionTypeEnum;
import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.Expense;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.Purchase;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.entity.Transaction;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.expense.ExpenseRq;
import com.triasoft.garage.model.expense.ExpenseRs;
import com.triasoft.garage.model.expense.ExpenseSummaryRs;
import com.triasoft.garage.model.purchase.PurchaseRs;
import com.triasoft.garage.projection.ExpenseMetrics;
import com.triasoft.garage.repository.ExpenseRepository;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.PurchaseRepository;
import com.triasoft.garage.repository.SaleRepository;
import com.triasoft.garage.repository.TransactionRepository;
import com.triasoft.garage.specifiction.ExpenseSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final AccountService accountService;
    private final PurchaseService purchaseService;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;
    private final PurchaseRepository purchaseRepository;
    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;
    private final JournalService journalService;

    public ExpenseRs getAll(Pageable pageable, UserDTO user) {
        Page<Expense> expensePage = expenseRepository.findByPurchaseIsNull(pageable);
        List<ExpenseDTO> expenses = expensePage.getContent().stream().map(this::convertToDTO).toList();
        ExpenseRs expenseRs = ExpenseRs.builder().expenses(expenses).build();
        expenseRs.setTotalPages(expensePage.getTotalPages());
        expenseRs.setTotalElements(expensePage.getTotalElements());
        return expenseRs;
    }

    public PurchaseRs getPurchasesWithExpenses(Pageable pageable, UserDTO user) {
        return purchaseService.getPurchasesWithExpenses(pageable, user);
    }

    public List<ExpenseDTO> getExpensesByPurchase(Long purchaseId, UserDTO user) {
        return expenseRepository.findByPurchaseId(purchaseId).stream()
                .map(this::convertToDTO)
                .toList();
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

    public ExpenseRs search(FilterRq filterRq, String type, Pageable pageable, UserDTO user) {
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

    @Transactional
    public ExpenseRs create(ExpenseRq expenseRq, UserDTO user) {
        if (expenseRq.getPaymentAccountId() == null) {
            throw new BusinessException(ErrorCode.Business.EXPENSE_PAYMENT_ACCOUNT_REQUIRED);
        }
        Expense expense = new Expense();
        expense.setDate(expenseRq.getDate());
        expense.setAmount(expenseRq.getAmount());
        expense.setDescription(expenseRq.getDescription());

        ExpenseDTO exDto = new ExpenseDTO();
        BeanUtils.copyProperties(expenseRq, exDto);
        expense.setExpenseAccount(accountService.getOrCreateExpenseAccount(exDto, user));

        PaymentAccount paymentAccount = null;
        if (expenseRq.getPaymentAccountId() != null) {
            paymentAccount = resolveAndValidateAccount(expenseRq.getPaymentAccountId(), expenseRq.getAmount());
            expense.setPaymentAccount(paymentAccount);
        }

        if (expenseRq.getPurchaseId() != null) {
            Purchase purchase = purchaseRepository.findById(expenseRq.getPurchaseId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND));
            guardSoldPurchaseExpenseWindow(purchase.getId());
            expense.setPurchase(purchase);
            Expense saved = expenseRepository.save(expense);

            purchase.setTotalAmount(purchase.getTotalAmount().add(expenseRq.getAmount()));
            purchaseRepository.save(purchase);

            Optional<Inventory> invOpt = inventoryRepository.findByPurchaseOrderDetailPurchaseId(purchase.getId());
            invOpt.ifPresent(inventory -> {
                inventory.setLandedCost(inventory.getLandedCost().add(expenseRq.getAmount()));
                inventoryRepository.save(inventory);
            });

            if (paymentAccount != null) {
                createTransaction(saved, TransactionTypeEnum.PURCHASE_EXPENSE_PAYMENT, "PURCHASE_EXPENSE",
                        purchase.getReferenceNo(), paymentAccount, TransactionDirectionEnum.OUT);
                journalService.post(JournalService.REF_EXPENSE, saved.getId());
            }

            invOpt.ifPresent(purchaseService::syncSaleAfterLandedCostChange);
        } else {
            Expense saved = expenseRepository.save(expense);
            if (paymentAccount != null) {
                createTransaction(saved, TransactionTypeEnum.EXPENSE, "EXPENSE",
                        null, paymentAccount, TransactionDirectionEnum.OUT);
                journalService.post(JournalService.REF_EXPENSE, saved.getId());
            }
        }

        return ExpenseRs.builder().build();
    }

    public ExpenseDTO get(Long id, UserDTO user) {
        Expense expense = expenseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));
        return convertToDTO(expense);
    }

    @Transactional
    public ExpenseRs update(Long id, ExpenseRq expenseRq, UserDTO user) {
        if (expenseRq.getPaymentAccountId() == null) {
            throw new BusinessException(ErrorCode.Business.EXPENSE_PAYMENT_ACCOUNT_REQUIRED);
        }
        Expense expense = expenseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));

        if (expense.getPurchase() != null) {
            guardSoldPurchaseExpenseWindow(expense.getPurchase().getId());
        }

        BigDecimal oldAmount = expense.getAmount();
        Long oldPaymentAccountId = expense.getPaymentAccount() != null ? expense.getPaymentAccount().getId() : null;

        boolean amountChanged = oldAmount.compareTo(expenseRq.getAmount()) != 0;
        boolean accountChanged = !Objects.equals(oldPaymentAccountId, expenseRq.getPaymentAccountId());

        expense.setDate(expenseRq.getDate());
        expense.setAmount(expenseRq.getAmount());
        expense.setDescription(expenseRq.getDescription());

        Long currentExpenseAccountId = expense.getExpenseAccount() != null ? expense.getExpenseAccount().getId() : null;
        if (!Objects.equals(currentExpenseAccountId, expenseRq.getTypeId())) {
            ExpenseDTO exDto = new ExpenseDTO();
            BeanUtils.copyProperties(expenseRq, exDto);
            expense.setExpenseAccount(accountService.getOrCreateExpenseAccount(exDto, user));
        }

        if (amountChanged || accountChanged) {
            reverseTransaction(expense);
            journalService.reverse(JournalService.REF_EXPENSE, id);
        }

        PaymentAccount newPaymentAccount = null;
        if (expenseRq.getPaymentAccountId() != null) {
            newPaymentAccount = resolveAndValidateAccount(expenseRq.getPaymentAccountId(), expenseRq.getAmount());
            expense.setPaymentAccount(newPaymentAccount);
        } else {
            expense.setPaymentAccount(null);
        }

        expenseRepository.save(expense);

        if (newPaymentAccount != null && (amountChanged || accountChanged)) {
            boolean isPurchaseLinked = expense.getPurchase() != null;
            createTransaction(expense,
                    isPurchaseLinked ? TransactionTypeEnum.PURCHASE_EXPENSE_PAYMENT : TransactionTypeEnum.EXPENSE,
                    isPurchaseLinked ? "PURCHASE_EXPENSE" : "EXPENSE",
                    isPurchaseLinked ? expense.getPurchase().getReferenceNo() : null,
                    newPaymentAccount, TransactionDirectionEnum.OUT);
            journalService.post(JournalService.REF_EXPENSE, expense.getId());
        }

        if (amountChanged && expense.getPurchase() != null) {
            BigDecimal delta = expenseRq.getAmount().subtract(oldAmount);
            Purchase purchase = expense.getPurchase();
            purchase.setTotalAmount(purchase.getTotalAmount().add(delta));
            purchaseRepository.save(purchase);
            Optional<Inventory> invOpt = inventoryRepository.findByPurchaseOrderDetailPurchaseId(purchase.getId());
            invOpt.ifPresent(inventory -> {
                inventory.setLandedCost(inventory.getLandedCost().add(delta));
                inventoryRepository.save(inventory);
            });
            invOpt.ifPresent(purchaseService::syncSaleAfterLandedCostChange);
        }

        return ExpenseRs.builder().build();
    }

    @Transactional
    public ExpenseRs delete(Long id, UserDTO user) {
        Expense expense = expenseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));

        if (expense.getPurchase() != null) {
            guardSoldPurchaseExpenseWindow(expense.getPurchase().getId());
        }

        reverseTransaction(expense);
        journalService.reverse(JournalService.REF_EXPENSE, id);

        if (expense.getPurchase() != null) {
            Purchase purchase = expense.getPurchase();
            purchase.setTotalAmount(purchase.getTotalAmount().subtract(expense.getAmount()));
            purchaseRepository.save(purchase);
            Optional<Inventory> invOpt = inventoryRepository.findByPurchaseOrderDetailPurchaseId(purchase.getId());
            invOpt.ifPresent(inventory -> {
                inventory.setLandedCost(inventory.getLandedCost().subtract(expense.getAmount()));
                inventoryRepository.save(inventory);
            });
            invOpt.ifPresent(purchaseService::syncSaleAfterLandedCostChange);
        }

        expenseRepository.delete(expense);
        return ExpenseRs.builder().build();
    }

    private void guardSoldPurchaseExpenseWindow(Long purchaseId) {
        inventoryRepository.findByPurchaseOrderDetailPurchaseId(purchaseId).ifPresent(inventory -> {
            if (!StatusEnum.SOLD.equals(inventory.getStatus())) return;
            Purchase purchase = purchaseRepository.findById(purchaseId).orElse(null);
            if (purchase == null || purchase.getPurchaseDetails().isEmpty()) return;
            var category = purchase.getPurchaseDetails().get(0).getProduct().getCategory();
            if (category == null || !category.isExpenseLockEnabled() || category.getExpenseLockWindow() == null) return;
            Sale sale = saleRepository.findByInventoryId(inventory.getId());
            if (sale == null) return;
            LocalDate deadline = computeExpenseDeadline(sale.getSaleDate(), category.getExpenseLockWindow());
            if (LocalDate.now().isAfter(deadline)) {
                throw new BusinessException(ErrorCode.Business.PURCHASE_EXPENSE_LOCKED);
            }
        });
    }

    static LocalDate computeExpenseDeadline(LocalDate saleDate, ExpenseLockWindow window) {
        return switch (window) {
            case IMMEDIATE -> saleDate.minusDays(1);
            case EOD -> saleDate;
            case EOM -> saleDate.withDayOfMonth(saleDate.lengthOfMonth());
            case EOQ -> {
                int quarterEndMonth = ((saleDate.getMonthValue() - 1) / 3 + 1) * 3;
                yield YearMonth.of(saleDate.getYear(), quarterEndMonth).atEndOfMonth();
            }
            case EOY -> LocalDate.of(saleDate.getYear(), 12, 31);
        };
    }

    private PaymentAccount resolveAndValidateAccount(Long accountId, BigDecimal amount) {
        PaymentAccount account = paymentAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));
        BigDecimal totalIn = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.IN);
        BigDecimal totalOut = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.OUT);
        BigDecimal balance = account.getOpeningBalance().add(totalIn).subtract(totalOut);
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.Business.INSUFFICIENT_BALANCE);
        }
        return account;
    }

    private void createTransaction(Expense expense, TransactionTypeEnum type, String referenceType,
                                   String description, PaymentAccount paymentAccount, TransactionDirectionEnum direction) {
        Transaction transaction = new Transaction();
        transaction.setTransactionDate(expense.getDate());
        transaction.setType(type);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(expense.getId());
        transaction.setPaymentAccount(paymentAccount);
        transaction.setAmount(expense.getAmount());
        transaction.setDirection(direction);
        transaction.setDescription(description != null
                ? expense.getExpenseAccount().getName() + " – " + description
                : expense.getExpenseAccount().getName());
        transactionRepository.save(transaction);
    }

    private void reverseTransaction(Expense expense) {
        Optional<Transaction> original = transactionRepository.findActiveByReferenceTypeAndReferenceId(
                expense.getPurchase() != null ? "PURCHASE_EXPENSE" : "EXPENSE", expense.getId());
        original.ifPresent(orig -> {
            if (transactionRepository.existsByReversalOfId(orig.getId())) return;
            Transaction reversal = new Transaction();
            reversal.setTransactionDate(LocalDate.now());
            reversal.setType(orig.getType());
            reversal.setReferenceType(orig.getReferenceType());
            reversal.setReferenceId(orig.getReferenceId());
            reversal.setPaymentAccount(orig.getPaymentAccount());
            reversal.setAmount(orig.getAmount());
            reversal.setDirection(TransactionDirectionEnum.IN);
            reversal.setDescription("Reversal – " + orig.getDescription());
            reversal.setReversalOf(orig);
            transactionRepository.save(reversal);
        });
    }

    private ExpenseDTO convertToDTO(Expense expense) {
        return ExpenseDTO.builder()
                .id(expense.getId())
                .date(expense.getDate())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .title(Objects.nonNull(expense.getExpenseAccount()) ? expense.getExpenseAccount().getName() : null)
                .typeId(Objects.nonNull(expense.getExpenseAccount()) ? expense.getExpenseAccount().getId() : null)
                .paymentAccountId(expense.getPaymentAccount() != null ? expense.getPaymentAccount().getId() : null)
                .purchaseId(expense.getPurchase() != null ? expense.getPurchase().getId() : null)
                .build();
    }
}
