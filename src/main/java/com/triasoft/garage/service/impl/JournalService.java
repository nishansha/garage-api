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

    // Party-subledger dimension values (see JournalDetail.partyType) — this business only
    // ever tags AR/AP-relevant lines against a Customer or a Vendor.
    public static final String PARTY_CUSTOMER = "CUSTOMER";
    public static final String PARTY_VENDOR = "VENDOR";

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

        Journal journal = ledgerService.createJournal(REF_SALE, saleId, sale.getSaleDate(),
                "Sale " + sale.getInvoiceNo() + " — " + sale.getCustomer().getName());

        List<JournalDetail> lines = new ArrayList<>();
        if (customerAR.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.AR), customerAR,
                    "Customer receivable — " + sale.getCustomer().getName(), PARTY_CUSTOMER, customerId));
        }
        if (finance.signum() > 0) {
            // Finance company isn't tracked as a structured party entity, so this line
            // carries no party tag - only customer/vendor receivables are subledgered today.
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.FINANCE_RECEIVABLE), finance,
                    "Finance receivable — " + sale.getFinanceCompany()));
        }
        if (exchange.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.INVENTORY), exchange,
                    "Trade-in vehicle received"));
        }
        if (landedCost.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.COGS), landedCost, "COGS at sale"));
        }
        lines.add(ledgerService.credit(journal, coa(SystemCoaRole.SALES_REVENUE), saleRate, "Sales revenue"));
        if (landedCost.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.INVENTORY), landedCost, "Inventory out"));
        }
        if (settlementPayable.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.CUSTOMER_SETTLEMENT_PAYABLE), settlementPayable,
                    "Customer settlement payable — " + sale.getCustomer().getName(), PARTY_CUSTOMER, customerId));
        }

        ledgerService.saveBalanced(lines);
    }

    private void handleSalePayment(Long paymentId) {
        SalePayment payment = salePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND));

        ChartOfAccount paymentCoa = paymentAccountCoa(payment.getPaymentAccount());
        boolean fromFinance = payment.getPayerType() != null
                && "FINANCE".equalsIgnoreCase(payment.getPayerType().name());
        ChartOfAccount creditAccount = fromFinance ? coa(SystemCoaRole.FINANCE_RECEIVABLE) : coa(SystemCoaRole.AR);
        String label = fromFinance ? "Finance disbursement" : "Customer payment";

        Journal journal = ledgerService.createJournal(REF_SALE_PAYMENT, paymentId, payment.getPaymentDate(),
                label + " for sale " + payment.getSale().getInvoiceNo());

        List<JournalDetail> lines = fromFinance
                ? List.of(
                    ledgerService.debit(journal, paymentCoa, payment.getAmount(), "Receipt to " + payment.getPaymentAccount().getName()),
                    ledgerService.credit(journal, creditAccount, payment.getAmount(), label))
                : List.of(
                    ledgerService.debit(journal, paymentCoa, payment.getAmount(), "Receipt to " + payment.getPaymentAccount().getName()),
                    ledgerService.credit(journal, creditAccount, payment.getAmount(), label, PARTY_CUSTOMER, payment.getSale().getCustomer().getId()));
        ledgerService.saveBalanced(lines);
    }

    private void handlePurchase(Long purchaseId) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND));

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

        Journal journal = ledgerService.createJournal(REF_PURCHASE, purchaseId, purchase.getOrderDate(),
                "Purchase " + purchase.getReferenceNo() + " — " + purchase.getVendor().getName());

        List<JournalDetail> lines = new ArrayList<>();
        lines.add(ledgerService.debit(journal, coa(SystemCoaRole.INVENTORY), baseAmount, "Inventory in (vehicle base)"));
        if (rcDue.signum() > 0) {
            // RCD is cash paid to the vendor on top of the vehicle price, recoverable once this
            // unit is resold — a receivable from day one, never part of Inventory/COGS.
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.RC_DUE_RECEIVABLE), rcDue,
                    "RC due receivable — " + purchase.getVendor().getName(), PARTY_VENDOR, vendorId));
        }
        lines.add(ledgerService.credit(journal, coa(SystemCoaRole.AP), baseAmount.add(rcDue),
                "Vendor payable — " + purchase.getVendor().getName(), PARTY_VENDOR, vendorId));
        ledgerService.saveBalanced(lines);
    }

    private void handlePurchasePayment(Long paymentId) {
        PurchasePayment payment = purchasePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND));

        ChartOfAccount paymentCoa = paymentAccountCoa(payment.getPaymentAccount());
        var exchangeInv = inventoryRepository.findByPurchaseOrderDetailPurchaseId(payment.getPurchase().getId())
                .filter(inv -> inv.getSourceSaleId() != null);
        boolean isExchange = exchangeInv.isPresent();
        ChartOfAccount debitAccount = isExchange ? coa(SystemCoaRole.CUSTOMER_SETTLEMENT_PAYABLE) : coa(SystemCoaRole.AP);
        String debitLabel = isExchange ? "Customer settlement payable cleared" : "Vendor payable cleared";
        String partyType = isExchange ? PARTY_CUSTOMER : PARTY_VENDOR;
        Long partyId = isExchange
                ? saleRepository.findById(exchangeInv.get().getSourceSaleId()).map(s -> s.getCustomer().getId()).orElse(null)
                : payment.getPurchase().getVendor().getId();

        Journal journal = ledgerService.createJournal(REF_PURCHASE_PAYMENT, paymentId, payment.getPaymentDate(),
                "Payment for purchase " + payment.getPurchase().getReferenceNo());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, debitAccount, payment.getAmount(), debitLabel, partyType, partyId),
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
        ChartOfAccount paymentCoa = paymentAccountCoa(expense.getPaymentAccount());
        // Purchase-linked expenses always capitalise into Inventory regardless of
        // whether the vehicle is sold. syncSaleAfterLandedCostChange() then reverse+
        // reposts the SALE journal (on the original sale date) so COGS in the sale
        // month automatically reflects the updated landed cost.
        ChartOfAccount debitAccount = expense.getPurchase() != null
                ? coa(SystemCoaRole.INVENTORY)
                : expense.getExpenseAccount();
        String label = expense.getPurchase() != null
                ? "Vehicle prep expense (capitalized)"
                : "Expense — " + debitAccount.getLabel();

        Journal journal = ledgerService.createJournal(REF_EXPENSE, expenseId, expense.getDate(),
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

        ChartOfAccount paymentCoa = paymentAccountCoa(entry.getPaymentAccount());
        ChartOfAccount offsetCoa = entry.getChartOfAccount();
        boolean isIn = "IN".equalsIgnoreCase(entry.getDirection().name());
        ChartOfAccount debitAccount = isIn ? paymentCoa : offsetCoa;
        ChartOfAccount creditAccount = isIn ? offsetCoa : paymentCoa;

        Journal journal = ledgerService.createJournal(REF_DIRECT_ENTRY, entryId, entry.getEntryDate(),
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

        Journal journal = ledgerService.createJournal(REF_SALE_RETURN, saleReturnId, sr.getReturnDate(),
                "Sale return for " + sale.getInvoiceNo() + " — " + sale.getCustomer().getName());

        List<JournalDetail> lines = new ArrayList<>();
        // Always: reverse revenue + restore sold inventory + reverse COGS
        if (saleRate.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.SALES_REVENUE), saleRate, "Reverse sales revenue"));
        }
        if (landedCostA.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.INVENTORY), landedCostA, "Sold vehicle back to inventory"));
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.COGS), landedCostA, "Reverse COGS"));
        }

        BigDecimal refundPayable;
        if (sr.getExchangeHandling() == ExchangeHandlingEnum.RETURN_TO_BUYER) {
            // Exchange vehicle goes back; expenses on car B become sunk loss; inventory of car B leaves.
            Inventory exchangeInv = inventoryRepository.findBySourceSaleId(sale.getId()).orElse(null);
            BigDecimal landedCostB = exchangeInv != null ? safe(exchangeInv.getLandedCost()) : exchange;
            BigDecimal expensesOnB = landedCostB.subtract(exchange);

            if (landedCostB.signum() > 0) {
                lines.add(ledgerService.credit(journal, coa(SystemCoaRole.INVENTORY), landedCostB, "Trade-in vehicle returned to buyer"));
            }
            if (expensesOnB.signum() > 0) {
                lines.add(ledgerService.debit(journal, coa(SystemCoaRole.LOSS_RETURNED_EXCHANGE), expensesOnB,
                        "Sunk expenses on returned trade-in"));
            }
            refundPayable = customerPaid.subtract(soldDed);
        } else if (sr.getExchangeHandling() == ExchangeHandlingEnum.KEEP_AND_BUYBACK) {
            BigDecimal buyback = safe(sr.getExchangeBuybackAmount());
            BigDecimal gain = exchange.subtract(buyback); // ≥ 0 due to cap (buyback ≤ exchange)
            // Cancel the SALE journal's DR Inventory for the exchange vehicle — it will be
            // re-entered at buyback price via a separate PURCHASE journal (postExchangeBuybackPurchase).
            if (exchange.signum() > 0) {
                lines.add(ledgerService.credit(journal, coa(SystemCoaRole.INVENTORY), exchange,
                        "Exchange vehicle reclassified to standalone purchase"));
            }
            if (gain.signum() > 0) {
                lines.add(ledgerService.credit(journal, coa(SystemCoaRole.GAIN_ON_EXCHANGE_ADJ), gain,
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
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.AR), outstandingAr,
                    "Cancel outstanding A/R from " + sale.getCustomer().getName(), PARTY_CUSTOMER, customerId));
        }
        // Record the new liability for cash we owe customer (separate from A/R so balance sheet is clean).
        if (refundPayable.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.CUSTOMER_REFUND_PAYABLE), refundPayable,
                    "Refund payable to " + sale.getCustomer().getName(), PARTY_CUSTOMER, customerId));
        }
        if (totalDed.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.RETURN_DEDUCTION_INCOME), totalDed,
                    "Return deduction income"));
        }
        ledgerService.saveBalanced(lines);
    }

    private void handleSaleReturnRefund(Long refundId) {
        SaleRefundPayment refund = saleRefundPaymentRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.REFUND_PAYMENT_NOT_FOUND));

        ChartOfAccount paymentCoa = paymentAccountCoa(refund.getPaymentAccount());
        Long customerId = refund.getSaleReturn().getSale().getCustomer().getId();
        Journal journal = ledgerService.createJournal(REF_SALE_RETURN_REFUND, refundId, refund.getPaymentDate(),
                "Refund payment for return of sale " + refund.getSaleReturn().getSale().getInvoiceNo());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, coa(SystemCoaRole.CUSTOMER_REFUND_PAYABLE), refund.getAmount(),
                        "Settle refund payable", PARTY_CUSTOMER, customerId),
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

        Journal journal = ledgerService.createJournal(REF_PURCHASE_RETURN, purchaseReturnId, pr.getReturnDate(),
                "Purchase return for inventory " + pr.getInventory().getUin() +
                        " (PO " + purchase.getReferenceNo() + ")");

        List<JournalDetail> lines = new ArrayList<>();
        if (apToCancel.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.AP), apToCancel,
                    "Cancel outstanding vendor A/P", PARTY_VENDOR, vendorId));
        }
        if (vendorReceivable.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.VENDOR_REFUND_RECEIVABLE), vendorReceivable,
                    "Refund receivable from " + purchase.getVendor().getName(), PARTY_VENDOR, vendorId));
        }
        if (loss.signum() > 0) {
            lines.add(ledgerService.debit(journal, coa(SystemCoaRole.LOSS_PURCHASE_RETURN), loss,
                    "Loss on purchase return (unrecovered cost)"));
        } else if (loss.signum() < 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.GAIN_ON_EXCHANGE_ADJ), loss.abs(),
                    "Gain on purchase return"));
        }
        if (landedCost.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.INVENTORY), landedCost, "Inventory out — returned to vendor"));
        }
        if (rcDue.signum() > 0) {
            lines.add(ledgerService.credit(journal, coa(SystemCoaRole.RC_DUE_RECEIVABLE), rcDue,
                    "Close RC due receivable — unit returned before resale", PARTY_VENDOR, vendorId));
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

        ChartOfAccount paymentCoa = paymentAccountCoa(receipt.getPaymentAccount());
        Long vendorId = receipt.getPurchaseReturn().getPurchase().getVendor().getId();
        Journal journal = ledgerService.createJournal(REF_PURCHASE_RETURN_RECEIPT, receiptId, receipt.getPaymentDate(),
                "Vendor refund receipt for PO " + receipt.getPurchaseReturn().getPurchase().getReferenceNo());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, paymentCoa, receipt.getAmount(),
                        "Refund received to " + receipt.getPaymentAccount().getName()),
                ledgerService.credit(journal, coa(SystemCoaRole.VENDOR_REFUND_RECEIVABLE), receipt.getAmount(),
                        "Settle vendor refund receivable", PARTY_VENDOR, vendorId)
        );
        ledgerService.saveBalanced(lines);
    }

    private void handleRcDueReceipt(Long receiptId) {
        RcDueReceipt receipt = rcDueReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.RC_DUE_RECEIPT_NOT_FOUND));

        ChartOfAccount paymentCoa = paymentAccountCoa(receipt.getPaymentAccount());
        Purchase purchase = receipt.getPurchase();
        Long vendorId = purchase.getVendor().getId();
        Journal journal = ledgerService.createJournal(REF_RC_DUE_RECEIPT, receiptId, receipt.getReceiptDate(),
                "RC due receipt from " + purchase.getVendor().getName() + " for purchase " + purchase.getReferenceNo());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, paymentCoa, receipt.getAmount(),
                        "RC due received to " + receipt.getPaymentAccount().getName()),
                ledgerService.credit(journal, coa(SystemCoaRole.RC_DUE_RECEIVABLE), receipt.getAmount(),
                        "Settle RC due receivable", PARTY_VENDOR, vendorId)
        );
        ledgerService.saveBalanced(lines);
    }

    private void handleOpeningBalance(Long paymentAccountId) {
        PaymentAccount account = paymentAccountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));

        BigDecimal amount = safe(account.getOpeningBalance());
        if (amount.signum() == 0) return; // nothing to post

        ChartOfAccount paymentCoa = paymentAccountCoa(account);
        Journal journal = ledgerService.createJournal(REF_OPENING_BALANCE, paymentAccountId, Objects.nonNull(account.getOpeningDate()) ? account.getOpeningDate() : LocalDate.now(),
                "Opening balance for " + account.getName());

        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, paymentCoa, amount, "Opening balance — " + account.getName()),
                ledgerService.credit(journal, coa(SystemCoaRole.OPENING_BALANCE_EQUITY), amount, "Opening Balance Equity")
        );
        ledgerService.saveBalanced(lines);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ChartOfAccount coa(SystemCoaRole role) {
        return ledgerService.findAccountBySystemRole(role.name());
    }

    private ChartOfAccount paymentAccountCoa(PaymentAccount account) {
        if (account == null || account.getChartOfAccount() == null) {
            throw new BusinessException(ErrorCode.Business.JOURNAL_PAYMENT_ACCOUNT_COA_MISSING);
        }
        return account.getChartOfAccount();
    }

    /**
     * Posts a PURCHASE journal for an exchange vehicle that the garage has decided
     * to keep after a KEEP_AND_BUYBACK sale return. The SALE_RETURN journal cancels
     * the original SALE's DR Inventory for the exchange vehicle; this journal
     * re-enters it at the buyback price and records the matching liability to the customer.
     */
    @Transactional
    public void postExchangeBuybackPurchase(Long purchaseId, BigDecimal buybackAmount,
                                             LocalDate journalDate, String customerName, Long customerId) {
        if (ledgerService.isPosted(REF_PURCHASE, purchaseId)) {
            return; // idempotent — already posted
        }
        Journal journal = ledgerService.createJournal(REF_PURCHASE, purchaseId, journalDate,
                "Exchange vehicle buyback — " + customerName);
        List<JournalDetail> lines = List.of(
                ledgerService.debit(journal, coa(SystemCoaRole.INVENTORY), buybackAmount,
                        "Exchange vehicle acquired at buyback price"),
                ledgerService.credit(journal, coa(SystemCoaRole.CUSTOMER_REFUND_PAYABLE), buybackAmount,
                        "Buyback payable to " + customerName, PARTY_CUSTOMER, customerId)
        );
        ledgerService.saveBalanced(lines);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
