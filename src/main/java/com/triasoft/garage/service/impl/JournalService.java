package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.ExchangeHandlingEnum;
import com.triasoft.garage.constants.SystemCoaRole;
import com.triasoft.garage.entity.*;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.ledger.entity.ChartOfAccount;
import com.triasoft.garage.ledger.entity.Journal;
import com.triasoft.garage.ledger.entity.JournalDetail;
import com.triasoft.garage.ledger.service.LedgerService;
import com.triasoft.garage.repository.*;
import com.triasoft.garage.servicesale.entity.ServiceSale;
import com.triasoft.garage.servicesale.entity.ServiceSalePayment;
import com.triasoft.garage.servicesale.repository.ServiceSalePaymentRepository;
import com.triasoft.garage.servicesale.repository.ServiceSaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Decides WHAT to post for each garage-api business event (RCD treatment, exchange/trade-in
 * handling, COGS from landed cost, etc) and delegates HOW to post it to the generic
 * {@link LedgerService}. This class is deliberately the dealership-specific half of the
 * ledger subsystem - see LedgerService/LedgerQueryService for the generic double-entry
 * mechanics that would move into a standalone reusable module.
 */
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
    public static final String REF_RC_DUE_RECEIPT = "RC_DUE_RECEIPT";
    public static final String REF_SERVICE_SALE = "SERVICE_SALE";
    public static final String REF_SERVICE_SALE_PAYMENT = "SERVICE_SALE_PAYMENT";
    public static final String REF_SALARY_PAYMENT = "SALARY_PAYMENT";

    // Party-subledger dimension values (see JournalDetail.partyType) — this business only
    // ever tags AR/AP-relevant lines against a Customer, a Vendor, or a FinanceCompany.
    public static final String PARTY_CUSTOMER = "CUSTOMER";
    public static final String PARTY_VENDOR = "VENDOR";
    public static final String PARTY_FINANCE = "FINANCE";
    public static final String PARTY_EMPLOYEE = "EMPLOYEE";

    // Open-item dimension values (see JournalDetail.sourceType) — every AR/AP-relevant line
    // traces back to either the Sale or the Purchase it originated from.
    public static final String SOURCE_SALE = "SALE";
    public static final String SOURCE_PURCHASE = "PURCHASE";
    public static final String SOURCE_SERVICE_SALE = "SERVICE_SALE";
    public static final String SOURCE_SALARY_PAYMENT = "SALARY_PAYMENT";

    private final LedgerService ledgerService;
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
    private final RcDueReceiptRepository rcDueReceiptRepository;
    private final ServiceSaleRepository serviceSaleRepository;
    private final ServiceSalePaymentRepository serviceSalePaymentRepository;
    private final com.triasoft.garage.hrm.repository.SalaryPaymentRepository salaryPaymentRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void post(String referenceType, Long referenceId) {
        if (ledgerService.isPosted(referenceType, referenceId)) {
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
            case REF_RC_DUE_RECEIPT           -> handleRcDueReceipt(referenceId);
            case REF_SERVICE_SALE             -> handleServiceSale(referenceId);
            case REF_SERVICE_SALE_PAYMENT     -> handleServiceSalePayment(referenceId);
            case REF_SALARY_PAYMENT           -> handleSalaryPayment(referenceId);
            default -> throw new BusinessException("JNL_400", "Unknown reference type: " + referenceType);
        }
    }

    @Transactional
    public Journal createManual(com.triasoft.garage.model.journal.JournalRq rq) {
        return ledgerService.createManual(rq);
    }

    @Transactional
    public void reverse(String referenceType, Long referenceId) {
        // Correction reversals (edit/delete of a record) always use the original
        // journal date so the correction stays in the same period it was posted.
        ledgerService.reverse(referenceType, referenceId);
    }

    @Transactional
    public void reverseOnDate(String referenceType, Long referenceId, LocalDate reversalDate) {
        // Payment/receipt cancellations use today — the cancellation is a current-period event.
        ledgerService.reverseOnDate(referenceType, referenceId, reversalDate);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Handlers — one per business event
    // ─────────────────────────────────────────────────────────────────────────

    private void handleSale(Long saleId) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SALE_NOT_FOUND));

        Long companyId = sale.getCompanyId();
        Long customerId = sale.getCustomer().getId();
        BigDecimal saleRate = safe(sale.getSaleRate());
        BigDecimal exchange = safe(sale.getExchangeAmount());
        BigDecimal finance = sale.isFinanced() ? safe(sale.getFinanceAmount()) : BigDecimal.ZERO;
        BigDecimal landedCost = safe(sale.getLandedCostAtSale());

        // RCD no longer touches the sale: it's captured and recognized entirely on the Purchase
        // side (see JournalService.handlePurchase) as a vendor receivable, independent of what
        // the customer pays here.
        BigDecimal beforeExchange = saleRate.subtract(exchange).subtract(finance);
        BigDecimal customerAR;
        BigDecimal settlementPayable;
        if (beforeExchange.signum() < 0) {
            customerAR = BigDecimal.ZERO;
            settlementPayable = beforeExchange.abs();
        } else {
            customerAR = beforeExchange;
            settlementPayable = BigDecimal.ZERO;
        }

        Journal journal = ledgerService.createJournal(REF_SALE, saleId, companyId, sale.getSaleDate(),
                "Sale " + sale.getInvoiceNo() + " — " + sale.getCustomer().getName());

        List<JournalDetail> lines = new ArrayList<>();
        if (customerAR.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.AR, companyId), customerAR,
                    "Customer receivable — " + sale.getCustomer().getName(), customerParty(customerId), saleSource(saleId)));
        }
        if (finance.signum() > 0) {
            // financeCompanyRef is only populated going forward (resolved from the free-text
            // financeCompany field) - historical sales predating this feature won't have it,
            // so this line is only tagged when it's available, same "no backfill" precedent
            // as the rest of the party/source tagging.
            String label = "Finance receivable — " + sale.getFinanceCompany();
            if (sale.getFinanceCompanyRef() != null) {
                lines.add(ledgerService.debit(journal, coa(SystemCoaRole.FINANCE_RECEIVABLE, companyId), finance, label,
                        financeParty(sale.getFinanceCompanyRef().getId()), saleSource(saleId)));
            } else {
                lines.add(ledgerService.debit(journal, coa(SystemCoaRole.FINANCE_RECEIVABLE, companyId), finance, label));
            }
        }
        if (exchange.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.INVENTORY, companyId), exchange,
                    "Trade-in vehicle received"));
        }
        if (landedCost.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.COGS, companyId), landedCost, "COGS at sale"));
        }
        lines.add(ledgerService.credit(journal, coa(SystemCoaRole.SALES_REVENUE, companyId), saleRate, "Sales revenue"));
        if (landedCost.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.INVENTORY, companyId), landedCost, "Inventory out"));
        }
        if (settlementPayable.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.CUSTOMER_SETTLEMENT_PAYABLE, companyId), settlementPayable,
                    "Customer settlement payable — " + sale.getCustomer().getName(), customerParty(customerId), saleSource(saleId)));
        }

        ledgerService.saveBalanced(lines);
    }

    private void handleSalePayment(Long paymentId) {
        SalePayment payment = salePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND));

        Long companyId = payment.getSale().getCompanyId();
        ChartOfAccount paymentCoa = paymentAccountCoa(payment.getPaymentAccount(), companyId);
        boolean fromFinance = payment.getPayerType() != null
                && "FINANCE".equalsIgnoreCase(payment.getPayerType().name());
        ChartOfAccount creditAccount = fromFinance ? coa(SystemCoaRole.FINANCE_RECEIVABLE, companyId) : coa(SystemCoaRole.AR, companyId);
        String label = fromFinance ? "Finance disbursement" : "Customer payment";

        Journal journal = ledgerService.createJournal(REF_SALE_PAYMENT, paymentId, companyId, payment.getPaymentDate(),
                label + " for sale " + payment.getSale().getInvoiceNo());

        FinanceCompany financeCompanyRef = fromFinance ? payment.getSale().getFinanceCompanyRef() : null;
        List<JournalDetail> lines = fromFinance
                ? (financeCompanyRef != null
                    ? List.of(
                        ledgerService.debit(journal, paymentCoa, payment.getAmount(), "Receipt to " + payment.getPaymentAccount().getName()),
                        ledgerService.credit(journal, creditAccount, payment.getAmount(), label,
                                financeParty(financeCompanyRef.getId()), saleSource(payment.getSale().getId())))
                    : List.of(
                        ledgerService.debit(journal, paymentCoa, payment.getAmount(), "Receipt to " + payment.getPaymentAccount().getName()),
                        ledgerService.credit(journal, creditAccount, payment.getAmount(), label)))
                : List.of(
                    ledgerService.debit(journal, paymentCoa, payment.getAmount(), "Receipt to " + payment.getPaymentAccount().getName()),
                    ledgerService.credit(journal, creditAccount, payment.getAmount(), label,
                            customerParty(payment.getSale().getCustomer().getId()), saleSource(payment.getSale().getId())));
        ledgerService.saveBalanced(lines);
    }

    private void handlePurchase(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND));

        Long companyId = purchase.getCompanyId();
        Long vendorId = purchase.getVendor().getId();
        // total_amount on Purchase includes the base vehicle price, linked expenses, AND any
        // refundable RCD paid to the vendor on top of the vehicle price. Purchase-linked expenses
        // post their own EXPENSE journals (DR Inventory / CR cash), so this PURCHASE journal must
        // strip both expenses and RCD out of the base (vendor-billed vehicle) amount to avoid:
        //  • double-debiting Inventory (once here, once in EXPENSE)
        //  • inflating A/P by expense amounts that were paid directly, not invoiced by vendor
        //  • landing RCD in Inventory, which would inflate landed cost/COGS with a recoverable amount
        BigDecimal expensesSum = purchase.getPurchaseExpenses().stream()
                .map(e -> safe(e.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rcDue = safe(purchase.getRcDueAmount());
        BigDecimal baseAmount = safe(purchase.getTotalAmount()).subtract(expensesSum).subtract(rcDue);

        Journal journal = ledgerService.createJournal(REF_PURCHASE, purchaseId, companyId, purchase.getOrderDate(),
                "Purchase " + purchase.getReferenceNo() + " — " + purchase.getVendor().getName());

        List<JournalDetail> lines = new ArrayList<>();
        lines.add(ledgerService.debit(journal, coa(SystemCoaRole.INVENTORY, companyId), baseAmount, "Inventory in (vehicle base)"));
        if (rcDue.signum() > 0) {
            // RCD is cash paid to the vendor on top of the vehicle price, recoverable once this
            // unit is resold — a receivable from day one, never part of Inventory/COGS.
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.RC_DUE_RECEIVABLE, companyId), rcDue,
                    "RC due receivable — " + purchase.getVendor().getName(), vendorParty(vendorId), purchaseSource(purchaseId)));
        }
        lines.add(ledgerService.credit(journal, coa(SystemCoaRole.AP, companyId), baseAmount.add(rcDue),
                "Vendor payable — " + purchase.getVendor().getName(), vendorParty(vendorId), purchaseSource(purchaseId)));
        ledgerService.saveBalanced(lines);
    }

    private void handlePurchasePayment(Long paymentId) {
        PurchasePayment payment = purchasePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND));

        Long companyId = payment.getPurchase().getCompanyId();
        ChartOfAccount paymentCoa = paymentAccountCoa(payment.getPaymentAccount(), companyId);
        var exchangeInv = inventoryRepository.findByPurchaseOrderDetailPurchaseId(payment.getPurchase().getId())
                .filter(inv -> inv.getSourceSaleId() != null);
        boolean isExchange = exchangeInv.isPresent();
        ChartOfAccount debitAccount = isExchange ? coa(SystemCoaRole.CUSTOMER_SETTLEMENT_PAYABLE, companyId) : coa(SystemCoaRole.AP, companyId);
        String debitLabel = isExchange ? "Customer settlement payable cleared" : "Vendor payable cleared";
        // The settlement payable being cleared here was booked in handleSale against the
        // ORIGINAL sale, not this purchase - party/source must trace back there, not to
        // this purchase, so it nets correctly against that sale's open item.
        LedgerService.Party party;
        LedgerService.Source source;
        if (isExchange) {
            Long saleId = exchangeInv.get().getSourceSaleId();
            Long customerId = saleRepository.findById(saleId).map(s -> s.getCustomer().getId()).orElse(null);
            party = customerParty(customerId);
            source = saleSource(saleId);
        } else {
            party = vendorParty(payment.getPurchase().getVendor().getId());
            source = purchaseSource(payment.getPurchase().getId());
        }

        Journal journal = ledgerService.createJournal(REF_PURCHASE_PAYMENT, paymentId, companyId, payment.getPaymentDate(),
                "Payment for purchase " + payment.getPurchase().getReferenceNo());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, debitAccount, payment.getAmount(), debitLabel, party, source),
                ledgerService.credit(journal, paymentCoa, payment.getAmount(),
                        "Cash out from " + payment.getPaymentAccount().getName())
        );
        ledgerService.saveBalanced(lines);
    }

    private void handleExpense(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));

        if (expense.getPaymentAccount() == null) {
            throw new BusinessException("JNL_411", "Expense must have a payment account to post journal");
        }
        // expenseAccount is required on every Expense and is itself company-scoped —
        // the reliable source of "which company's books" regardless of whether this
        // expense is purchase-linked or standalone.
        Long companyId = expense.getExpenseAccount().getCompanyId();
        ChartOfAccount paymentCoa = paymentAccountCoa(expense.getPaymentAccount(), companyId);
        // Purchase-linked expenses always capitalise into Inventory regardless of
        // whether the vehicle is sold. syncSaleAfterLandedCostChange() then reverse+
        // reposts the SALE journal (on the original sale date) so COGS in the sale
        // month automatically reflects the updated landed cost.
        ChartOfAccount debitAccount = expense.getPurchase() != null
                ? coa(SystemCoaRole.INVENTORY, companyId)
                : expense.getExpenseAccount();
        String label = expense.getPurchase() != null
                ? "Vehicle prep expense (capitalized)"
                : "Expense — " + debitAccount.getLabel();

        Journal journal = ledgerService.createJournal(REF_EXPENSE, expenseId, companyId, expense.getDate(),
                label + (expense.getDescription() != null ? " — " + expense.getDescription() : ""));

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, debitAccount, expense.getAmount(), label),
                ledgerService.credit(journal, paymentCoa, expense.getAmount(),
                        "Cash out from " + expense.getPaymentAccount().getName())
        );
        ledgerService.saveBalanced(lines);
    }

    private void handleDirectEntry(Long entryId) {
        DirectEntry entry = directEntryRepository.findById(entryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.DIRECT_ENTRY_NOT_FOUND));

        ChartOfAccount offsetCoa = entry.getChartOfAccount();
        Long companyId = offsetCoa.getCompanyId();
        ChartOfAccount paymentCoa = paymentAccountCoa(entry.getPaymentAccount(), companyId);
        boolean isIn = "IN".equalsIgnoreCase(entry.getDirection().name());
        ChartOfAccount debitAccount = isIn ? paymentCoa : offsetCoa;
        ChartOfAccount creditAccount = isIn ? offsetCoa : paymentCoa;

        Journal journal = ledgerService.createJournal(REF_DIRECT_ENTRY, entryId, companyId, entry.getEntryDate(),
                offsetCoa.getLabel() + (entry.getPartyName() != null ? " — " + entry.getPartyName() : ""));

        // entry.getPartyName() is free text (no Customer/Vendor FK) - not a structured
        // party, so these lines carry no party tag.
        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, debitAccount, entry.getAmount(), offsetCoa.getLabel()),
                ledgerService.credit(journal, creditAccount, entry.getAmount(), offsetCoa.getLabel())
        );
        ledgerService.saveBalanced(lines);
    }

    private void handleSaleReturn(Long saleReturnId) {
        SaleReturn sr = saleReturnRepository.findById(saleReturnId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SALE_RETURN_NOT_FOUND));
        Sale sale = sr.getSale();
        Long companyId = sale.getCompanyId();
        Long customerId = sale.getCustomer().getId();

        BigDecimal saleRate = safe(sale.getSaleRate());
        BigDecimal landedCostA = safe(sale.getLandedCostAtSale());
        BigDecimal customerPaid = safe(sr.getCustomerPaidAmount());
        BigDecimal exchange = safe(sale.getExchangeAmount());
        BigDecimal soldDed = safe(sr.getSoldVehicleDeductionAmount());
        BigDecimal exchDed = safe(sr.getExchangeVehicleDeductionAmount());
        BigDecimal totalDed = soldDed.add(exchDed);
        // Outstanding receivable to cancel = whatever the customer still owed us in cash before return.
        BigDecimal outstandingAr = saleRate.subtract(exchange).subtract(customerPaid).max(BigDecimal.ZERO);

        Journal journal = ledgerService.createJournal(REF_SALE_RETURN, saleReturnId, companyId, sr.getReturnDate(),
                "Sale return for " + sale.getInvoiceNo() + " — " + sale.getCustomer().getName());

        List<JournalDetail> lines = new ArrayList<>();
        // Always: reverse revenue + restore sold inventory + reverse COGS
        if (saleRate.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.SALES_REVENUE, companyId), saleRate, "Reverse sales revenue"));
        }
        if (landedCostA.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.INVENTORY, companyId), landedCostA, "Sold vehicle back to inventory"));
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.COGS, companyId), landedCostA, "Reverse COGS"));
        }

        BigDecimal refundPayable;
        if (sr.getExchangeHandling() == ExchangeHandlingEnum.RETURN_TO_BUYER) {
            // Exchange vehicle goes back; expenses on car B become sunk loss; inventory of car B leaves.
            Inventory exchangeInv = inventoryRepository.findBySourceSaleId(sale.getId()).orElse(null);
            BigDecimal landedCostB = exchangeInv != null ? safe(exchangeInv.getLandedCost()) : exchange;
            BigDecimal expensesOnB = landedCostB.subtract(exchange);

            if (landedCostB.signum() > 0) {
                lines.add(ledgerService.credit(journal, coa(SystemCoaRole.INVENTORY, companyId), landedCostB, "Trade-in vehicle returned to buyer"));
            }
            if (expensesOnB.signum() > 0) {
                lines.add(ledgerService.debit(journal, coa(SystemCoaRole.LOSS_RETURNED_EXCHANGE, companyId), expensesOnB,
                        "Sunk expenses on returned trade-in"));
            }
            refundPayable = customerPaid.subtract(soldDed);
        } else if (sr.getExchangeHandling() == ExchangeHandlingEnum.KEEP_AND_BUYBACK) {
            BigDecimal buyback = safe(sr.getExchangeBuybackAmount());
            BigDecimal gain = exchange.subtract(buyback); // ≥ 0 due to cap (buyback ≤ exchange)
            // Cancel the SALE journal's DR Inventory for the exchange vehicle — it will be
            // re-entered at buyback price via a separate PURCHASE journal (postExchangeBuybackPurchase).
            if (exchange.signum() > 0) {
                lines.add(ledgerService.credit(journal, coa(SystemCoaRole.INVENTORY, companyId), exchange,
                        "Exchange vehicle reclassified to standalone purchase"));
            }
            if (gain.signum() > 0) {
                lines.add(ledgerService.credit(journal, coa(SystemCoaRole.GAIN_ON_EXCHANGE_ADJ, companyId), gain,
                        "Gain on exchange buyback renegotiation"));
            }
            // Buyback liability is recorded in the companion PURCHASE journal, not here.
            refundPayable = customerPaid.subtract(totalDed);
        } else {
            // NONE
            refundPayable = customerPaid.subtract(soldDed);
        }

        // Cancel any outstanding A/R the customer still owed us (the sale is unwound).
        if (outstandingAr.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.AR, companyId), outstandingAr,
                    "Cancel outstanding A/R from " + sale.getCustomer().getName(), customerParty(customerId), saleSource(sale.getId())));
        }
        // Record the new liability for cash we owe customer (separate from A/R so balance sheet is clean).
        if (refundPayable.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.CUSTOMER_REFUND_PAYABLE, companyId), refundPayable,
                    "Refund payable to " + sale.getCustomer().getName(), customerParty(customerId), saleSource(sale.getId())));
        }
        if (totalDed.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.RETURN_DEDUCTION_INCOME, companyId), totalDed,
                    "Return deduction income"));
        }
        ledgerService.saveBalanced(lines);
    }

    private void handleSaleReturnRefund(Long refundId) {
        SaleRefundPayment refund = saleRefundPaymentRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.REFUND_PAYMENT_NOT_FOUND));

        Long companyId = refund.getSaleReturn().getSale().getCompanyId();
        ChartOfAccount paymentCoa = paymentAccountCoa(refund.getPaymentAccount(), companyId);
        Long customerId = refund.getSaleReturn().getSale().getCustomer().getId();
        Long saleId = refund.getSaleReturn().getSale().getId();
        Journal journal = ledgerService.createJournal(REF_SALE_RETURN_REFUND, refundId, companyId, refund.getPaymentDate(),
                "Refund payment for return of sale " + refund.getSaleReturn().getSale().getInvoiceNo());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, coa(SystemCoaRole.CUSTOMER_REFUND_PAYABLE, companyId), refund.getAmount(),
                        "Settle refund payable", customerParty(customerId), saleSource(saleId)),
                ledgerService.credit(journal, paymentCoa, refund.getAmount(),
                        "Refund paid from " + refund.getPaymentAccount().getName())
        );
        ledgerService.saveBalanced(lines);
    }

    private void handlePurchaseReturn(Long purchaseReturnId) {
        PurchaseReturn pr = purchaseReturnRepository.findById(purchaseReturnId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_RETURN_NOT_FOUND));

        // Stored `returnAmount` holds the unwind value (= outstandingAp + refundAmount).
        // Split it for proper presentation: A/P cancellation goes to A/P (liability),
        // refund-from-vendor goes to a dedicated asset account so the balance sheet doesn't
        // show A/P with a debit balance.
        Purchase purchase = pr.getPurchase();
        Long companyId = purchase.getCompanyId();
        Long vendorId = purchase.getVendor().getId();
        BigDecimal unwindAmount = safe(pr.getReturnAmount());
        BigDecimal landedCost = safe(pr.getInventoryLandedCost());
        // A return can only happen before this unit is ever sold (see PurchaseReturnService),
        // and an RC due receipt can only be recorded after — so any rcDueAmount here is always
        // fully un-received. It must unwind alongside the vehicle cost, not leak into loss/gain:
        // the RCD was never part of landedCost, so it isn't part of what the vendor "restocks"
        // either — it's closed out 1:1 against the receivable that was booked for it at purchase.
        BigDecimal rcDue = safe(purchase.getRcDueAmount());
        BigDecimal vendorInvoice = computeVendorInvoiceAmount(purchase);
        BigDecimal paidToVendor = safe(purchasePaymentRepository.sumAmountByPurchaseId(purchase.getId()));
        BigDecimal outstandingAp = vendorInvoice.subtract(paidToVendor).max(BigDecimal.ZERO);
        // Cap at unwindAmount in the (unlikely) edge case that outstandingAp drifted higher.
        BigDecimal apToCancel = outstandingAp.min(unwindAmount);
        BigDecimal vendorReceivable = unwindAmount.subtract(apToCancel).max(BigDecimal.ZERO);
        BigDecimal loss = landedCost.add(rcDue).subtract(unwindAmount); // sunk expenses + restocking fee

        Journal journal = ledgerService.createJournal(REF_PURCHASE_RETURN, purchaseReturnId, companyId, pr.getReturnDate(),
                "Purchase return for inventory " + pr.getInventory().getUin() +
                        " (PO " + purchase.getReferenceNo() + ")");

        List<JournalDetail> lines = new ArrayList<>();
        if (apToCancel.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.AP, companyId), apToCancel,
                    "Cancel outstanding vendor A/P", vendorParty(vendorId), purchaseSource(purchase.getId())));
        }
        if (vendorReceivable.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.VENDOR_REFUND_RECEIVABLE, companyId), vendorReceivable,
                    "Refund receivable from " + purchase.getVendor().getName(), vendorParty(vendorId), purchaseSource(purchase.getId())));
        }
        if (loss.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.LOSS_PURCHASE_RETURN, companyId), loss,
                    "Loss on purchase return (unrecovered cost)"));
        } else if (loss.signum() < 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.GAIN_ON_EXCHANGE_ADJ, companyId), loss.abs(),
                    "Gain on purchase return"));
        }
        if (landedCost.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.INVENTORY, companyId), landedCost, "Inventory out — returned to vendor"));
        }
        if (rcDue.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.RC_DUE_RECEIVABLE, companyId), rcDue,
                    "Close RC due receivable — unit returned before resale", vendorParty(vendorId), purchaseSource(purchase.getId())));
        }
        ledgerService.saveBalanced(lines);
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
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_RETURN_RECEIPT_NOT_FOUND));

        Purchase purchase = receipt.getPurchaseReturn().getPurchase();
        Long companyId = purchase.getCompanyId();
        ChartOfAccount paymentCoa = paymentAccountCoa(receipt.getPaymentAccount(), companyId);
        Long vendorId = purchase.getVendor().getId();
        Journal journal = ledgerService.createJournal(REF_PURCHASE_RETURN_RECEIPT, receiptId, companyId, receipt.getPaymentDate(),
                "Vendor refund receipt for PO " + purchase.getReferenceNo());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, paymentCoa, receipt.getAmount(),
                        "Refund received to " + receipt.getPaymentAccount().getName()),
                ledgerService.credit(journal, coa(SystemCoaRole.VENDOR_REFUND_RECEIVABLE, companyId), receipt.getAmount(),
                        "Settle vendor refund receivable", vendorParty(vendorId), purchaseSource(purchase.getId()))
        );
        ledgerService.saveBalanced(lines);
    }

    private void handleRcDueReceipt(Long receiptId) {
        RcDueReceipt receipt = rcDueReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.RC_DUE_RECEIPT_NOT_FOUND));

        Purchase purchase = receipt.getPurchase();
        Long companyId = purchase.getCompanyId();
        ChartOfAccount paymentCoa = paymentAccountCoa(receipt.getPaymentAccount(), companyId);
        Long vendorId = purchase.getVendor().getId();
        Journal journal = ledgerService.createJournal(REF_RC_DUE_RECEIPT, receiptId, companyId, receipt.getReceiptDate(),
                "RC due receipt from " + purchase.getVendor().getName() + " for purchase " + purchase.getReferenceNo());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, paymentCoa, receipt.getAmount(),
                        "RC due received to " + receipt.getPaymentAccount().getName()),
                ledgerService.credit(journal, coa(SystemCoaRole.RC_DUE_RECEIVABLE, companyId), receipt.getAmount(),
                        "Settle RC due receivable", vendorParty(vendorId), purchaseSource(purchase.getId()))
        );
        ledgerService.saveBalanced(lines);
    }

    private void handleOpeningBalance(Long paymentAccountId) {
        PaymentAccount account = paymentAccountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));

        BigDecimal amount = safe(account.getOpeningBalance());
        if (amount.signum() == 0) return; // nothing to post

        ChartOfAccount paymentCoa = paymentAccountCoa(account);
        Long companyId = paymentCoa.getCompanyId();
        Journal journal = ledgerService.createJournal(REF_OPENING_BALANCE, paymentAccountId, companyId, Objects.nonNull(account.getOpeningDate()) ? account.getOpeningDate() : LocalDate.now(),
                "Opening balance for " + account.getName());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, paymentCoa, amount, "Opening balance — " + account.getName()),
                ledgerService.credit(journal, coa(SystemCoaRole.OPENING_BALANCE_EQUITY, companyId), amount, "Opening Balance Equity")
        );
        ledgerService.saveBalanced(lines);
    }

    // Mirrors handleSale/handleSalePayment, minus COGS/Inventory/exchange/finance —
    // a service sale has no product/inventory involved, just revenue + AR.
    private void handleServiceSale(Long serviceSaleId) {
        ServiceSale sale = serviceSaleRepository.findById(serviceSaleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SERVICE_SALE_NOT_FOUND));

        Long companyId = sale.getCompanyId();
        BigDecimal totalAmount = safe(sale.getTotalAmount());
        Journal journal = ledgerService.createJournal(REF_SERVICE_SALE, serviceSaleId, companyId, sale.getSaleDate(),
                "Service sale " + sale.getInvoiceNo() + " — " + sale.customerDisplayName());

        List<JournalDetail> lines = new ArrayList<>();
        // customer is nullable (walk-in) — only tag the party dimension when there's a
        // real Customer to trace back to, same "no backfill for untracked parties"
        // convention as the rest of this class.
        if (sale.getCustomer() != null) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.AR, companyId), totalAmount,
                    "Customer receivable — " + sale.customerDisplayName(),
                    customerParty(sale.getCustomer().getId()), serviceSaleSource(serviceSaleId)));
        } else {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.AR, companyId), totalAmount,
                    "Customer receivable — " + sale.customerDisplayName(), null, serviceSaleSource(serviceSaleId)));
        }
        lines.add(ledgerService.credit(journal, coa(SystemCoaRole.SERVICE_REVENUE, companyId), totalAmount, "Service revenue"));
        ledgerService.saveBalanced(lines);
    }

    private void handleServiceSalePayment(Long paymentId) {
        ServiceSalePayment payment = serviceSalePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND));

        ServiceSale sale = payment.getServiceSale();
        Long companyId = sale.getCompanyId();
        ChartOfAccount paymentCoa = paymentAccountCoa(payment.getPaymentAccount(), companyId);
        Journal journal = ledgerService.createJournal(REF_SERVICE_SALE_PAYMENT, paymentId, companyId, payment.getPaymentDate(),
                "Payment for service sale " + sale.getInvoiceNo());

        LedgerService.Party party = sale.getCustomer() != null ? customerParty(sale.getCustomer().getId()) : null;
        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, paymentCoa, payment.getAmount(), "Receipt to " + payment.getPaymentAccount().getName()),
                ledgerService.credit(journal, coa(SystemCoaRole.AR, companyId), payment.getAmount(), "Customer payment",
                        party, serviceSaleSource(sale.getId()))
        );
        ledgerService.saveBalanced(lines);
    }

    // Posted only when a SalaryPayment is marked PAID (not at generation, which creates
    // PENDING rows) — see SalaryRunScheduler/SalaryPaymentService.markPaid.
    private void handleSalaryPayment(Long salaryPaymentId) {
        com.triasoft.garage.hrm.entity.SalaryPayment payment = salaryPaymentRepository.findById(salaryPaymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SALARY_PAYMENT_NOT_FOUND));

        Long companyId = payment.getEmployee().getCompanyId();
        ChartOfAccount paymentCoa = paymentAccountCoa(payment.getPaymentAccount(), companyId);
        Journal journal = ledgerService.createJournal(REF_SALARY_PAYMENT, salaryPaymentId, companyId, payment.getPaymentDate(),
                "Salary — " + payment.getEmployee().getName() + " (" + payment.getPayPeriodMonth() + "/" + payment.getPayPeriodYear() + ")");

        LedgerService.Party party = new LedgerService.Party(PARTY_EMPLOYEE, payment.getEmployee().getId());
        LedgerService.Source source = new LedgerService.Source(SOURCE_SALARY_PAYMENT, salaryPaymentId);
        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, coa(SystemCoaRole.SALARY_EXPENSE, companyId), payment.getNetAmount(),
                        "Salary expense — " + payment.getEmployee().getName(), party, source),
                ledgerService.credit(journal, paymentCoa, payment.getNetAmount(),
                        "Salary paid from " + payment.getPaymentAccount().getName())
        );
        ledgerService.saveBalanced(lines);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ChartOfAccount coa(SystemCoaRole role, Long companyId) {
        return ledgerService.findAccountBySystemRole(role.name(), companyId);
    }

    private LedgerService.Party customerParty(Long customerId) {
        return new LedgerService.Party(PARTY_CUSTOMER, customerId);
    }

    private LedgerService.Party vendorParty(Long vendorId) {
        return new LedgerService.Party(PARTY_VENDOR, vendorId);
    }

    private LedgerService.Party financeParty(Long financeCompanyId) {
        return new LedgerService.Party(PARTY_FINANCE, financeCompanyId);
    }

    private LedgerService.Source saleSource(Long saleId) {
        return new LedgerService.Source(SOURCE_SALE, saleId);
    }

    private LedgerService.Source purchaseSource(Long purchaseId) {
        return new LedgerService.Source(SOURCE_PURCHASE, purchaseId);
    }

    private LedgerService.Source serviceSaleSource(Long serviceSaleId) {
        return new LedgerService.Source(SOURCE_SERVICE_SALE, serviceSaleId);
    }

    // Used only by handleOpeningBalance, where the payment account itself is establishing the
    // company (there's no independent transaction to check it against).
    private ChartOfAccount paymentAccountCoa(PaymentAccount account) {
        if (account == null || account.getChartOfAccount() == null) {
            throw new BusinessException(ErrorCode.Business.JOURNAL_PAYMENT_ACCOUNT_COA_MISSING);
        }
        return account.getChartOfAccount();
    }

    // Every other handler already knows the transaction's companyId independently (from the
    // Sale/Purchase/Expense/etc. it's posting for) - this guards against a payment account from
    // a different company being selected, which would otherwise silently blend two companies'
    // cash positions (PaymentAccount has its own companyId; nothing upstream enforces the picker
    // only shows same-company accounts, so this is the last line of defense).
    private ChartOfAccount paymentAccountCoa(PaymentAccount account, Long companyId) {
        ChartOfAccount coa = paymentAccountCoa(account);
        if (!Objects.equals(account.getCompanyId(), companyId)) {
            throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_COMPANY_MISMATCH);
        }
        return coa;
    }

    /**
     * Posts a PURCHASE journal for an exchange vehicle that the garage has decided
     * to keep after a KEEP_AND_BUYBACK sale return. The SALE_RETURN journal cancels
     * the original SALE's DR Inventory for the exchange vehicle; this journal
     * re-enters it at the buyback price and records the matching liability to the customer.
     */
    @Transactional
    public void postExchangeBuybackPurchase(Long purchaseId, Long companyId, BigDecimal buybackAmount,
                                             LocalDate journalDate, String customerName, Long customerId, Long saleId) {
        if (ledgerService.isPosted(REF_PURCHASE, purchaseId)) {
            return; // idempotent — already posted
        }
        Journal journal = ledgerService.createJournal(REF_PURCHASE, purchaseId, companyId, journalDate,
                "Exchange vehicle buyback — " + customerName);
        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, coa(SystemCoaRole.INVENTORY, companyId), buybackAmount,
                        "Exchange vehicle acquired at buyback price"),
                // This payable traces back to the ORIGINAL sale/return (the customer relationship
                // that created it), not this buyback purchase itself - same convention as the
                // exchange branch of handlePurchasePayment.
                ledgerService.credit(journal, coa(SystemCoaRole.CUSTOMER_REFUND_PAYABLE, companyId), buybackAmount,
                        "Buyback payable to " + customerName, customerParty(customerId), saleSource(saleId))
        );
        ledgerService.saveBalanced(lines);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
