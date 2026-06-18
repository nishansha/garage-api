package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.JournalStatusEnum;
import com.triasoft.garage.entity.*;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class JournalService {

    // Reference types — also used by callers (services) when invoking post/reverse
    public static final String REF_SALE = "SALE";
    public static final String REF_SALE_PAYMENT = "SALE_PAYMENT";
    public static final String REF_PURCHASE = "PURCHASE";
    public static final String REF_PURCHASE_PAYMENT = "PURCHASE_PAYMENT";
    public static final String REF_EXPENSE = "EXPENSE";
    public static final String REF_DIRECT_ENTRY = "DIRECT_ENTRY";
    public static final String REF_OPENING_BALANCE = "OPENING_BALANCE";
    public static final String REF_MANUAL_JOURNAL = "MANUAL_JOURNAL";

    // CoA codes for system-managed accounts
    private static final String COA_AR = "1100";
    private static final String COA_FINANCE_RECEIVABLE = "1150";
    private static final String COA_INVENTORY = "1200";
    private static final String COA_AP = "2000";
    private static final String COA_CUSTOMER_SETTLEMENT_PAYABLE = "2400";
    private static final String COA_OPENING_BALANCE_EQUITY = "3900";
    private static final String COA_SALES_REVENUE = "4000";
    private static final String COA_COGS = "5000";

    private final JournalRepository journalRepository;
    private final JournalDetailRepository journalDetailRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final SaleRepository saleRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final ExpenseRepository expenseRepository;
    private final DirectEntryRepository directEntryRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final InventoryRepository inventoryRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void post(String referenceType, Long referenceId) {
        if (journalRepository.findActiveByReferenceTypeAndReferenceId(referenceType, referenceId).isPresent()) {
            throw new BusinessException(ErrorCode.Business.JOURNAL_ALREADY_POSTED);
        }
        switch (referenceType) {
            case REF_SALE             -> handleSale(referenceId);
            case REF_SALE_PAYMENT     -> handleSalePayment(referenceId);
            case REF_PURCHASE         -> handlePurchase(referenceId);
            case REF_PURCHASE_PAYMENT -> handlePurchasePayment(referenceId);
            case REF_EXPENSE          -> handleExpense(referenceId);
            case REF_DIRECT_ENTRY     -> handleDirectEntry(referenceId);
            case REF_OPENING_BALANCE  -> handleOpeningBalance(referenceId);
            default -> throw new BusinessException("JNL_400", "Unknown reference type: " + referenceType);
        }
    }

    @Transactional
    public Journal createManual(com.triasoft.garage.model.journal.JournalRq rq) {
        if (rq.getLines() == null || rq.getLines().size() < 2) {
            throw new BusinessException("JNL_420", "Journal must have at least 2 lines");
        }
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        for (var lineRq : rq.getLines()) {
            BigDecimal dr = safe(lineRq.getDebitAmount());
            BigDecimal cr = safe(lineRq.getCreditAmount());
            if (dr.signum() > 0 && cr.signum() > 0) {
                throw new BusinessException("JNL_421", "Each line must have debit OR credit, not both");
            }
            if (dr.signum() == 0 && cr.signum() == 0) {
                throw new BusinessException("JNL_422", "Each line must have a non-zero debit or credit");
            }
            if (lineRq.getAccountId() == null) {
                throw new BusinessException("JNL_423", "Each line must specify an accountId");
            }
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }
        if (totalDr.compareTo(totalCr) != 0) {
            throw new BusinessException(ErrorCode.Business.JOURNAL_NOT_BALANCED);
        }

        Journal journal = new Journal();
        journal.setJournalDate(rq.getJournalDate() != null ? rq.getJournalDate() : LocalDate.now());
        journal.setReferenceType(REF_MANUAL_JOURNAL);
        journal.setReferenceId(0L); // placeholder, set to own id after save
        journal.setDescription(rq.getDescription() != null ? rq.getDescription() : "Manual journal entry");
        journal.setStatus(JournalStatusEnum.POSTED);
        journal = journalRepository.save(journal);
        journal.setReferenceId(journal.getId());
        journal = journalRepository.save(journal);

        List<JournalDetail> lines = new ArrayList<>();
        for (var lineRq : rq.getLines()) {
            ChartOfAccount account = chartOfAccountRepository.findById(lineRq.getAccountId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.JOURNAL_COA_MISSING));
            JournalDetail line = new JournalDetail();
            line.setJournal(journal);
            line.setAccount(account);
            line.setDebitAmount(safe(lineRq.getDebitAmount()));
            line.setCreditAmount(safe(lineRq.getCreditAmount()));
            line.setDescription(lineRq.getDescription());
            lines.add(line);
        }
        journalDetailRepository.saveAll(lines);
        return journal;
    }

    @Transactional
    public void reverse(String referenceType, Long referenceId) {
        Journal original = journalRepository.findActiveByReferenceTypeAndReferenceId(referenceType, referenceId)
                .orElse(null);
        if (original == null) return; // nothing to reverse

        List<JournalDetail> originalLines = journalDetailRepository.findByJournalId(original.getId());

        Journal reversal = new Journal();
        reversal.setJournalDate(LocalDate.now());
        reversal.setReferenceType(referenceType);
        reversal.setReferenceId(referenceId);
        reversal.setDescription("Reversal of: " + original.getDescription());
        reversal.setStatus(JournalStatusEnum.POSTED);
        reversal.setReversalOf(original);
        reversal = journalRepository.save(reversal);

        for (JournalDetail line : originalLines) {
            JournalDetail rev = new JournalDetail();
            rev.setJournal(reversal);
            rev.setAccount(line.getAccount());
            rev.setDebitAmount(line.getCreditAmount());   // swap
            rev.setCreditAmount(line.getDebitAmount());   // swap
            rev.setDescription("Reversal: " + (line.getDescription() != null ? line.getDescription() : ""));
            journalDetailRepository.save(rev);
        }

        original.setStatus(JournalStatusEnum.REVERSED);
        journalRepository.save(original);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Handlers — one per business event
    // ─────────────────────────────────────────────────────────────────────────

    private void handleSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found: " + saleId));

        BigDecimal saleRate = safe(sale.getSaleRate());
        BigDecimal exchange = safe(sale.getExchangeAmount());
        BigDecimal finance = sale.isFinanced() ? safe(sale.getFinanceAmount()) : BigDecimal.ZERO;
        BigDecimal landedCost = safe(sale.getLandedCostAtSale());
        BigDecimal customerAR = saleRate.subtract(exchange).subtract(finance);

        Journal journal = createJournal(REF_SALE, saleId, sale.getSaleDate(),
                "Sale " + sale.getInvoiceNo() + " — " + sale.getCustomer().getName());

        List<JournalDetail> lines = new ArrayList<>();
        if (customerAR.signum() > 0) {
            lines.add(debit(journal, coa(COA_AR), customerAR, "Customer receivable — " + sale.getCustomer().getName()));
        }
        if (finance.signum() > 0) {
            lines.add(debit(journal, coa(COA_FINANCE_RECEIVABLE), finance,
                    "Finance receivable — " + sale.getFinanceCompany()));
        }
        if (exchange.signum() > 0) {
            lines.add(debit(journal, coa(COA_INVENTORY), exchange,
                    "Trade-in vehicle received"));
        }
        if (landedCost.signum() > 0) {
            lines.add(debit(journal, coa(COA_COGS), landedCost, "COGS at sale"));
        }
        lines.add(credit(journal, coa(COA_SALES_REVENUE), saleRate, "Sales revenue"));
        if (landedCost.signum() > 0) {
            lines.add(credit(journal, coa(COA_INVENTORY), landedCost, "Inventory out"));
        }
        if (customerAR.signum() < 0) {
            lines.add(credit(journal, coa(COA_CUSTOMER_SETTLEMENT_PAYABLE), customerAR.abs(),
                    "Customer settlement payable — " + sale.getCustomer().getName()));
        }

        saveBalanced(lines);
    }

    private void handleSalePayment(Long paymentId) {
        SalePayment payment = salePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Sale payment not found: " + paymentId));

        ChartOfAccount paymentCoa = paymentAccountCoa(payment.getPaymentAccount());
        boolean fromFinance = payment.getPayerType() != null
                && "FINANCE".equalsIgnoreCase(payment.getPayerType().name());
        ChartOfAccount creditAccount = fromFinance ? coa(COA_FINANCE_RECEIVABLE) : coa(COA_AR);
        String label = fromFinance ? "Finance disbursement" : "Customer payment";

        Journal journal = createJournal(REF_SALE_PAYMENT, paymentId, payment.getPaymentDate(),
                label + " for sale " + payment.getSale().getInvoiceNo());

        List<JournalDetail> lines = List.of(
                debit(journal, paymentCoa, payment.getAmount(),
                        "Receipt to " + payment.getPaymentAccount().getName()),
                credit(journal, creditAccount, payment.getAmount(), label)
        );
        saveBalanced(lines);
    }

    private void handlePurchase(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase not found: " + purchaseId));

        // total_amount on Purchase includes both the base vehicle price AND linked expenses.
        // Purchase-linked expenses post their own EXPENSE journals (DR Inventory / CR cash),
        // so this PURCHASE journal must use only the base (vendor-billed) amount to avoid:
        //  • double-debiting Inventory (once here, once in EXPENSE)
        //  • inflating A/P by expense amounts that were paid directly, not invoiced by vendor
        BigDecimal expensesSum = purchase.getPurchaseExpenses().stream()
                .map(e -> safe(e.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal baseAmount = safe(purchase.getTotalAmount()).subtract(expensesSum);

        Journal journal = createJournal(REF_PURCHASE, purchaseId, purchase.getOrderDate(),
                "Purchase " + purchase.getReferenceNo() + " — " + purchase.getVendor().getName());

        List<JournalDetail> lines = List.of(
                debit(journal, coa(COA_INVENTORY), baseAmount, "Inventory in (vehicle base)"),
                credit(journal, coa(COA_AP), baseAmount, "Vendor payable — " + purchase.getVendor().getName())
        );
        saveBalanced(lines);
    }

    private void handlePurchasePayment(Long paymentId) {
        PurchasePayment payment = purchasePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase payment not found: " + paymentId));

        ChartOfAccount paymentCoa = paymentAccountCoa(payment.getPaymentAccount());
        boolean isExchange = inventoryRepository.findByPurchaseOrderDetailPurchaseId(payment.getPurchase().getId())
                .map(inv -> inv.getSourceSaleId() != null)
                .orElse(false);
        ChartOfAccount debitAccount = isExchange ? coa(COA_CUSTOMER_SETTLEMENT_PAYABLE) : coa(COA_AP);
        String debitLabel = isExchange ? "Customer settlement payable cleared" : "Vendor payable cleared";
        Journal journal = createJournal(REF_PURCHASE_PAYMENT, paymentId, payment.getPaymentDate(),
                "Payment for purchase " + payment.getPurchase().getReferenceNo());

        List<JournalDetail> lines = List.of(
                debit(journal, debitAccount, payment.getAmount(), debitLabel),
                credit(journal, paymentCoa, payment.getAmount(),
                        "Cash out from " + payment.getPaymentAccount().getName())
        );
        saveBalanced(lines);
    }

    private void handleExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Expense not found: " + expenseId));

        if (expense.getPaymentAccount() == null) {
            throw new BusinessException("JNL_411", "Expense must have a payment account to post journal");
        }
        ChartOfAccount paymentCoa = paymentAccountCoa(expense.getPaymentAccount());
        // If linked to a purchase → capitalize into inventory (landed cost).
        // Otherwise → debit the expense CoA selected by user.
        ChartOfAccount debitAccount = expense.getPurchase() != null
                ? coa(COA_INVENTORY)
                : expense.getExpenseAccount();
        String label = expense.getPurchase() != null
                ? "Vehicle prep expense (capitalized)"
                : "Expense — " + debitAccount.getLabel();

        Journal journal = createJournal(REF_EXPENSE, expenseId, expense.getDate(),
                label + (expense.getDescription() != null ? " — " + expense.getDescription() : ""));

        List<JournalDetail> lines = List.of(
                debit(journal, debitAccount, expense.getAmount(), label),
                credit(journal, paymentCoa, expense.getAmount(),
                        "Cash out from " + expense.getPaymentAccount().getName())
        );
        saveBalanced(lines);
    }

    private void handleDirectEntry(Long entryId) {
        DirectEntry entry = directEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("Direct entry not found: " + entryId));

        ChartOfAccount paymentCoa = paymentAccountCoa(entry.getPaymentAccount());
        ChartOfAccount offsetCoa = entry.getChartOfAccount();
        boolean isIn = "IN".equalsIgnoreCase(entry.getDirection().name());
        ChartOfAccount debitAccount = isIn ? paymentCoa : offsetCoa;
        ChartOfAccount creditAccount = isIn ? offsetCoa : paymentCoa;

        Journal journal = createJournal(REF_DIRECT_ENTRY, entryId, entry.getEntryDate(),
                offsetCoa.getLabel() + (entry.getPartyName() != null ? " — " + entry.getPartyName() : ""));

        List<JournalDetail> lines = List.of(
                debit(journal, debitAccount, entry.getAmount(), offsetCoa.getLabel()),
                credit(journal, creditAccount, entry.getAmount(), offsetCoa.getLabel())
        );
        saveBalanced(lines);
    }

    private void handleOpeningBalance(Long paymentAccountId) {
        PaymentAccount account = paymentAccountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Payment account not found: " + paymentAccountId));

        BigDecimal amount = safe(account.getOpeningBalance());
        if (amount.signum() == 0) return; // nothing to post

        ChartOfAccount paymentCoa = paymentAccountCoa(account);
        Journal journal = createJournal(REF_OPENING_BALANCE, paymentAccountId, Objects.nonNull(account.getOpeningDate()) ? account.getOpeningDate() : LocalDate.now(),
                "Opening balance for " + account.getName());

        List<JournalDetail> lines = List.of(
                debit(journal, paymentCoa, amount, "Opening balance — " + account.getName()),
                credit(journal, coa(COA_OPENING_BALANCE_EQUITY), amount, "Opening Balance Equity")
        );
        saveBalanced(lines);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Journal createJournal(String referenceType, Long referenceId, LocalDate date, String description) {
        Journal journal = new Journal();
        journal.setJournalDate(date != null ? date : LocalDate.now());
        journal.setReferenceType(referenceType);
        journal.setReferenceId(referenceId);
        journal.setDescription(description);
        journal.setStatus(JournalStatusEnum.POSTED);
        return journalRepository.save(journal);
    }

    private JournalDetail debit(Journal journal, ChartOfAccount account, BigDecimal amount, String description) {
        JournalDetail d = new JournalDetail();
        d.setJournal(journal);
        d.setAccount(account);
        d.setDebitAmount(amount);
        d.setCreditAmount(BigDecimal.ZERO);
        d.setDescription(description);
        return d;
    }

    private JournalDetail credit(Journal journal, ChartOfAccount account, BigDecimal amount, String description) {
        JournalDetail c = new JournalDetail();
        c.setJournal(journal);
        c.setAccount(account);
        c.setDebitAmount(BigDecimal.ZERO);
        c.setCreditAmount(amount);
        c.setDescription(description);
        return c;
    }

    private void saveBalanced(List<JournalDetail> lines) {
        BigDecimal totalDebit = lines.stream().map(JournalDetail::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalDetail::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException(ErrorCode.Business.JOURNAL_NOT_BALANCED);
        }
        journalDetailRepository.saveAll(lines);
    }

    private ChartOfAccount coa(String code) {
        return chartOfAccountRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.JOURNAL_COA_MISSING));
    }

    private ChartOfAccount paymentAccountCoa(PaymentAccount account) {
        if (account == null || account.getChartOfAccount() == null) {
            throw new BusinessException(ErrorCode.Business.JOURNAL_PAYMENT_ACCOUNT_COA_MISSING);
        }
        return account.getChartOfAccount();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
