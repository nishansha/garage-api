package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.ExchangeHandlingEnum;
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
    public static final String REF_SALE_RETURN = "SALE_RETURN";
    public static final String REF_SALE_RETURN_REFUND = "SALE_RETURN_REFUND";
    public static final String REF_PURCHASE_RETURN = "PURCHASE_RETURN";
    public static final String REF_PURCHASE_RETURN_RECEIPT = "PURCHASE_RETURN_RECEIPT";

    // CoA codes for system-managed accounts
    private static final String COA_AR = "1100";
    private static final String COA_FINANCE_RECEIVABLE = "1150";
    private static final String COA_VENDOR_REFUND_RECEIVABLE = "1170";
    private static final String COA_INVENTORY = "1200";
    private static final String COA_AP = "2000";
    private static final String COA_CUSTOMER_SETTLEMENT_PAYABLE = "2400";
    private static final String COA_CUSTOMER_REFUND_PAYABLE = "2410";
    private static final String COA_OPENING_BALANCE_EQUITY = "3900";
    private static final String COA_SALES_REVENUE = "4000";
    private static final String COA_COGS = "5000";
    private static final String COA_RETURN_DEDUCTION_INCOME = "4520";
    private static final String COA_GAIN_ON_EXCHANGE_ADJ = "4530";
    private static final String COA_LOSS_RETURNED_EXCHANGE = "5510";
    private static final String COA_LOSS_PURCHASE_RETURN = "5520";

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
    private final SaleReturnRepository saleReturnRepository;
    private final SaleRefundPaymentRepository saleRefundPaymentRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseReturnReceiptRepository purchaseReturnReceiptRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void post(String referenceType, Long referenceId) {
        if (journalRepository.findActiveByReferenceTypeAndReferenceId(referenceType, referenceId).isPresent()) {
            throw new BusinessException(ErrorCode.Business.JOURNAL_ALREADY_POSTED);
        }
        switch (referenceType) {
            case REF_SALE                    -> handleSale(referenceId);
            case REF_SALE_PAYMENT            -> handleSalePayment(referenceId);
            case REF_PURCHASE                -> handlePurchase(referenceId);
            case REF_PURCHASE_PAYMENT        -> handlePurchasePayment(referenceId);
            case REF_EXPENSE                 -> handleExpense(referenceId);
            case REF_DIRECT_ENTRY            -> handleDirectEntry(referenceId);
            case REF_OPENING_BALANCE         -> handleOpeningBalance(referenceId);
            case REF_SALE_RETURN             -> handleSaleReturn(referenceId);
            case REF_SALE_RETURN_REFUND      -> handleSaleReturnRefund(referenceId);
            case REF_PURCHASE_RETURN         -> handlePurchaseReturn(referenceId);
            case REF_PURCHASE_RETURN_RECEIPT -> handlePurchaseReturnReceipt(referenceId);
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

    private void handleSaleReturn(Long saleReturnId) {
        SaleReturn sr = saleReturnRepository.findById(saleReturnId)
                .orElseThrow(() -> new EntityNotFoundException("Sale return not found: " + saleReturnId));
        Sale sale = sr.getSale();

        BigDecimal saleRate = safe(sale.getSaleRate());
        BigDecimal landedCostA = safe(sale.getLandedCostAtSale());
        BigDecimal customerPaid = safe(sr.getCustomerPaidAmount());
        BigDecimal exchange = safe(sale.getExchangeAmount());
        BigDecimal soldDed = safe(sr.getSoldVehicleDeductionAmount());
        BigDecimal exchDed = safe(sr.getExchangeVehicleDeductionAmount());
        BigDecimal totalDed = soldDed.add(exchDed);
        // Outstanding receivable to cancel = whatever the customer still owed us in cash before return.
        BigDecimal outstandingAr = saleRate.subtract(exchange).subtract(customerPaid).max(BigDecimal.ZERO);

        Journal journal = createJournal(REF_SALE_RETURN, saleReturnId, sr.getReturnDate(),
                "Sale return for " + sale.getInvoiceNo() + " — " + sale.getCustomer().getName());

        List<JournalDetail> lines = new ArrayList<>();
        // Always: reverse revenue + restore sold inventory + reverse COGS
        if (saleRate.signum() > 0) {
            lines.add(debit(journal, coa(COA_SALES_REVENUE), saleRate, "Reverse sales revenue"));
        }
        if (landedCostA.signum() > 0) {
            lines.add(debit(journal, coa(COA_INVENTORY), landedCostA, "Sold vehicle back to inventory"));
            lines.add(credit(journal, coa(COA_COGS), landedCostA, "Reverse COGS"));
        }

        BigDecimal refundPayable;
        if (sr.getExchangeHandling() == ExchangeHandlingEnum.RETURN_TO_BUYER) {
            // Exchange vehicle goes back; expenses on car B become sunk loss; inventory of car B leaves.
            Inventory exchangeInv = inventoryRepository.findBySourceSaleId(sale.getId()).orElse(null);
            BigDecimal landedCostB = exchangeInv != null ? safe(exchangeInv.getLandedCost()) : exchange;
            BigDecimal expensesOnB = landedCostB.subtract(exchange);

            if (landedCostB.signum() > 0) {
                lines.add(credit(journal, coa(COA_INVENTORY), landedCostB, "Trade-in vehicle returned to buyer"));
            }
            if (expensesOnB.signum() > 0) {
                lines.add(debit(journal, coa(COA_LOSS_RETURNED_EXCHANGE), expensesOnB,
                        "Sunk expenses on returned trade-in"));
            }
            refundPayable = customerPaid.subtract(soldDed);
        } else if (sr.getExchangeHandling() == ExchangeHandlingEnum.KEEP_AND_BUYBACK) {
            BigDecimal buyback = safe(sr.getExchangeBuybackAmount());
            BigDecimal gain = exchange.subtract(buyback); // ≥ 0 due to cap (buyback ≤ exchange)
            if (gain.signum() > 0) {
                lines.add(credit(journal, coa(COA_GAIN_ON_EXCHANGE_ADJ), gain,
                        "Gain on exchange buyback renegotiation"));
            }
            refundPayable = customerPaid.add(buyback).subtract(totalDed);
        } else {
            // NONE
            refundPayable = customerPaid.subtract(soldDed);
        }

        // Cancel any outstanding A/R the customer still owed us (the sale is unwound).
        if (outstandingAr.signum() > 0) {
            lines.add(credit(journal, coa(COA_AR), outstandingAr,
                    "Cancel outstanding A/R from " + sale.getCustomer().getName()));
        }
        // Record the new liability for cash we owe customer (separate from A/R so balance sheet is clean).
        if (refundPayable.signum() > 0) {
            lines.add(credit(journal, coa(COA_CUSTOMER_REFUND_PAYABLE), refundPayable,
                    "Refund payable to " + sale.getCustomer().getName()));
        }
        if (totalDed.signum() > 0) {
            lines.add(credit(journal, coa(COA_RETURN_DEDUCTION_INCOME), totalDed,
                    "Return deduction income"));
        }
        saveBalanced(lines);
    }

    private void handleSaleReturnRefund(Long refundId) {
        SaleRefundPayment refund = saleRefundPaymentRepository.findById(refundId)
                .orElseThrow(() -> new EntityNotFoundException("Sale refund payment not found: " + refundId));

        ChartOfAccount paymentCoa = paymentAccountCoa(refund.getPaymentAccount());
        Journal journal = createJournal(REF_SALE_RETURN_REFUND, refundId, refund.getPaymentDate(),
                "Refund payment for return of sale " + refund.getSaleReturn().getSale().getInvoiceNo());

        List<JournalDetail> lines = List.of(
                debit(journal, coa(COA_CUSTOMER_REFUND_PAYABLE), refund.getAmount(),
                        "Settle refund payable"),
                credit(journal, paymentCoa, refund.getAmount(),
                        "Refund paid from " + refund.getPaymentAccount().getName())
        );
        saveBalanced(lines);
    }

    private void handlePurchaseReturn(Long purchaseReturnId) {
        PurchaseReturn pr = purchaseReturnRepository.findById(purchaseReturnId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase return not found: " + purchaseReturnId));

        // Stored `returnAmount` holds the unwind value (= outstandingAp + refundAmount).
        // Split it for proper presentation: A/P cancellation goes to A/P (liability),
        // refund-from-vendor goes to a dedicated asset account so the balance sheet doesn't
        // show A/P with a debit balance.
        Purchase purchase = pr.getPurchase();
        BigDecimal unwindAmount = safe(pr.getReturnAmount());
        BigDecimal landedCost = safe(pr.getInventoryLandedCost());
        BigDecimal vendorInvoice = computeVendorInvoiceAmount(purchase);
        BigDecimal paidToVendor = safe(purchasePaymentRepository.sumAmountByPurchaseId(purchase.getId()));
        BigDecimal outstandingAp = vendorInvoice.subtract(paidToVendor).max(BigDecimal.ZERO);
        // Cap at unwindAmount in the (unlikely) edge case that outstandingAp drifted higher.
        BigDecimal apToCancel = outstandingAp.min(unwindAmount);
        BigDecimal vendorReceivable = unwindAmount.subtract(apToCancel).max(BigDecimal.ZERO);
        BigDecimal loss = landedCost.subtract(unwindAmount); // sunk expenses + restocking fee

        Journal journal = createJournal(REF_PURCHASE_RETURN, purchaseReturnId, pr.getReturnDate(),
                "Purchase return for inventory " + pr.getInventory().getUin() +
                        " (PO " + purchase.getReferenceNo() + ")");

        List<JournalDetail> lines = new ArrayList<>();
        if (apToCancel.signum() > 0) {
            lines.add(debit(journal, coa(COA_AP), apToCancel, "Cancel outstanding vendor A/P"));
        }
        if (vendorReceivable.signum() > 0) {
            lines.add(debit(journal, coa(COA_VENDOR_REFUND_RECEIVABLE), vendorReceivable,
                    "Refund receivable from " + purchase.getVendor().getName()));
        }
        if (loss.signum() > 0) {
            lines.add(debit(journal, coa(COA_LOSS_PURCHASE_RETURN), loss,
                    "Loss on purchase return (unrecovered cost)"));
        } else if (loss.signum() < 0) {
            lines.add(credit(journal, coa(COA_GAIN_ON_EXCHANGE_ADJ), loss.abs(),
                    "Gain on purchase return"));
        }
        if (landedCost.signum() > 0) {
            lines.add(credit(journal, coa(COA_INVENTORY), landedCost, "Inventory out — returned to vendor"));
        }
        saveBalanced(lines);
    }

    /** Vendor's net invoice = purchase total − sum of expenses (which post their own journals). */
    private BigDecimal computeVendorInvoiceAmount(Purchase purchase) {
        BigDecimal expensesSum = expenseRepository.findByPurchaseId(purchase.getId()).stream()
                .map(e -> safe(e.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return safe(purchase.getTotalAmount()).subtract(expensesSum).max(BigDecimal.ZERO);
    }

    private void handlePurchaseReturnReceipt(Long receiptId) {
        PurchaseReturnReceipt receipt = purchaseReturnReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new EntityNotFoundException("Purchase return receipt not found: " + receiptId));

        ChartOfAccount paymentCoa = paymentAccountCoa(receipt.getPaymentAccount());
        Journal journal = createJournal(REF_PURCHASE_RETURN_RECEIPT, receiptId, receipt.getPaymentDate(),
                "Vendor refund receipt for PO " + receipt.getPurchaseReturn().getPurchase().getReferenceNo());

        List<JournalDetail> lines = List.of(
                debit(journal, paymentCoa, receipt.getAmount(),
                        "Refund received to " + receipt.getPaymentAccount().getName()),
                credit(journal, coa(COA_VENDOR_REFUND_RECEIVABLE), receipt.getAmount(),
                        "Settle vendor refund receivable")
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
