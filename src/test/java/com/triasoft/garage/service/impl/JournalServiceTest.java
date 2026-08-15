package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.SystemCoaRole;
import com.triasoft.garage.entity.Customer;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.Purchase;
import com.triasoft.garage.entity.PurchaseDetail;
import com.triasoft.garage.entity.RcDueReceipt;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.entity.Vendor;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.ledger.constants.JournalStatusEnum;
import com.triasoft.garage.ledger.entity.ChartOfAccount;
import com.triasoft.garage.ledger.entity.Journal;
import com.triasoft.garage.ledger.entity.JournalDetail;
import com.triasoft.garage.ledger.repository.ChartOfAccountRepository;
import com.triasoft.garage.ledger.repository.JournalDetailRepository;
import com.triasoft.garage.ledger.repository.JournalRepository;
import com.triasoft.garage.ledger.service.LedgerService;
import com.triasoft.garage.model.journal.JournalLineRq;
import com.triasoft.garage.model.journal.JournalRq;
import com.triasoft.garage.repository.DirectEntryRepository;
import com.triasoft.garage.repository.ExpenseRepository;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.PurchasePaymentRepository;
import com.triasoft.garage.repository.PurchaseRepository;
import com.triasoft.garage.repository.PurchaseReturnReceiptRepository;
import com.triasoft.garage.repository.PurchaseReturnRepository;
import com.triasoft.garage.repository.RcDueReceiptRepository;
import com.triasoft.garage.repository.SalePaymentRepository;
import com.triasoft.garage.repository.SaleRefundPaymentRepository;
import com.triasoft.garage.repository.SaleRepository;
import com.triasoft.garage.repository.SaleReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the double-entry posting/reversal logic in {@link JournalService}.
 * JournalService now delegates the generic post/reverse/balance mechanics to
 * {@link LedgerService} - these tests use a REAL LedgerService (constructed with mocked
 * ledger repositories) as JournalService's collaborator, so the "which accounts/amounts"
 * business-logic assertions below still exercise the actual posting code path end to end,
 * without needing a real DB.
 */
@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock private JournalRepository journalRepository;
    @Mock private JournalDetailRepository journalDetailRepository;
    @Mock private ChartOfAccountRepository chartOfAccountRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private SalePaymentRepository salePaymentRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private PurchasePaymentRepository purchasePaymentRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private DirectEntryRepository directEntryRepository;
    @Mock private PaymentAccountRepository paymentAccountRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private SaleReturnRepository saleReturnRepository;
    @Mock private SaleRefundPaymentRepository saleRefundPaymentRepository;
    @Mock private PurchaseReturnRepository purchaseReturnRepository;
    @Mock private PurchaseReturnReceiptRepository purchaseReturnReceiptRepository;
    @Mock private RcDueReceiptRepository rcDueReceiptRepository;

    private JournalService journalService;

    @BeforeEach
    void setUp() {
        LedgerService ledgerService = new LedgerService(journalRepository, journalDetailRepository, chartOfAccountRepository);
        journalService = new JournalService(
                ledgerService,
                saleRepository, salePaymentRepository, purchaseRepository, purchasePaymentRepository,
                expenseRepository, directEntryRepository, paymentAccountRepository, inventoryRepository,
                saleReturnRepository, saleRefundPaymentRepository, purchaseReturnRepository,
                purchaseReturnReceiptRepository, rcDueReceiptRepository);

        AtomicLong journalIdSeq = new AtomicLong(1);
        lenient().when(journalRepository.save(any(Journal.class))).thenAnswer(inv -> {
            Journal journal = inv.getArgument(0);
            if (journal.getId() == null) {
                journal.setId(journalIdSeq.getAndIncrement());
            }
            return journal;
        });
        lenient().when(journalDetailRepository.save(any(JournalDetail.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(journalDetailRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ChartOfAccount coaFor(SystemCoaRole role) {
        ChartOfAccount coa = new ChartOfAccount();
        coa.setId((long) (role.ordinal() + 1));
        coa.setLabel(role.name());
        coa.setSystemRole(role.name());
        when(chartOfAccountRepository.findBySystemRole(role.name())).thenReturn(Optional.of(coa));
        return coa;
    }

    private Sale buildSale(BigDecimal saleRate, BigDecimal exchangeAmount, boolean financed,
                            BigDecimal financeAmount, BigDecimal landedCost) {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("John Doe");

        Sale sale = new Sale();
        sale.setId(1L);
        sale.setInvoiceNo("INV-1");
        sale.setCustomer(customer);
        sale.setSaleDate(LocalDate.of(2026, 1, 10));
        sale.setSaleRate(saleRate);
        sale.setExchangeAmount(exchangeAmount);
        sale.setFinanced(financed);
        sale.setFinanceAmount(financeAmount);
        sale.setLandedCostAtSale(landedCost);
        return sale;
    }

    private Purchase buildPurchase(BigDecimal purchaseRate, BigDecimal rcDueAmount) {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setName("Acme Motors");

        Purchase purchase = new Purchase();
        purchase.setId(1L);
        purchase.setReferenceNo("PO-1");
        purchase.setVendor(vendor);
        purchase.setOrderDate(LocalDate.of(2026, 1, 5));
        purchase.setTotalAmount(purchaseRate.add(safeOrZero(rcDueAmount)));
        purchase.setRcDueAmount(rcDueAmount);
        return purchase;
    }

    private BigDecimal safeOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Test
    void post_throwsWhenJournalAlreadyPostedForReference() {
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE, 1L))
                .thenReturn(Optional.of(new Journal()));

        assertThatThrownBy(() -> journalService.post(JournalService.REF_SALE, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("BUS_132"); // JOURNAL_ALREADY_POSTED

        verifyNoInteractions(saleRepository);
    }

    @Test
    void post_throwsOnUnknownReferenceType() {
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(anyString(), anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> journalService.post("SOMETHING_ELSE", 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("JNL_400");
    }


    @Test
    void post_sale_basicCase_postsBalancedJournalWithCustomerReceivable() {
        Sale sale = buildSale(
                new BigDecimal("100000"), BigDecimal.ZERO, false, null, new BigDecimal("70000"));
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE, 1L))
                .thenReturn(Optional.empty());

        coaFor(SystemCoaRole.AR);
        coaFor(SystemCoaRole.COGS);
        coaFor(SystemCoaRole.SALES_REVENUE);
        coaFor(SystemCoaRole.INVENTORY);

        journalService.post(JournalService.REF_SALE, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        List<JournalDetail> lines = captor.getValue();

        BigDecimal totalDebit = lines.stream().map(JournalDetail::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalDetail::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit);
        assertThat(totalDebit).isEqualByComparingTo("170000");

        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.AR.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("100000");
            // Part 2: AR line must be tagged with the customer for the party subledger.
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_CUSTOMER);
            assertThat(line.getPartyId()).isEqualTo(1L);
            // Part 3: also tagged with the sale as the open-item source.
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_SALE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.SALES_REVENUE.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("100000");
            // Revenue lines aren't a per-party balance - no party/source tag.
            assertThat(line.getPartyType()).isNull();
            assertThat(line.getSourceType()).isNull();
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.COGS.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("70000");
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.INVENTORY.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("70000");
        });
    }

    @Test
    void post_sale_exchangeExceedsSaleRate_creditsCustomerSettlementPayableInsteadOfReceivable() {
        // Trade-in worth more than the sale price: customer is owed money, not the other way round.
        Sale sale = buildSale(
                new BigDecimal("50000"), new BigDecimal("60000"), false, null, BigDecimal.ZERO);
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE, 1L))
                .thenReturn(Optional.empty());

        coaFor(SystemCoaRole.INVENTORY);
        coaFor(SystemCoaRole.SALES_REVENUE);
        coaFor(SystemCoaRole.CUSTOMER_SETTLEMENT_PAYABLE);

        journalService.post(JournalService.REF_SALE, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        List<JournalDetail> lines = captor.getValue();

        BigDecimal totalDebit = lines.stream().map(JournalDetail::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalDetail::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit);

        assertThat(lines).noneMatch(line -> SystemCoaRole.AR.name().equals(
                line.getAccount() != null ? line.getAccount().getLabel() : null));
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.CUSTOMER_SETTLEMENT_PAYABLE.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("10000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_CUSTOMER);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_SALE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void post_sale_financed_tagsFinanceReceivableWithFinanceCompanyAndSale() {
        Sale sale = buildSale(
                new BigDecimal("100000"), BigDecimal.ZERO, true, new BigDecimal("60000"), new BigDecimal("70000"));
        com.triasoft.garage.entity.FinanceCompany financeCompany = new com.triasoft.garage.entity.FinanceCompany();
        financeCompany.setId(5L);
        financeCompany.setName("Bajaj Finance");
        sale.setFinanceCompanyRef(financeCompany);
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE, 1L))
                .thenReturn(Optional.empty());

        coaFor(SystemCoaRole.AR);
        coaFor(SystemCoaRole.FINANCE_RECEIVABLE);
        coaFor(SystemCoaRole.COGS);
        coaFor(SystemCoaRole.SALES_REVENUE);
        coaFor(SystemCoaRole.INVENTORY);

        journalService.post(JournalService.REF_SALE, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        List<JournalDetail> lines = captor.getValue();

        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.FINANCE_RECEIVABLE.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("60000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_FINANCE);
            assertThat(line.getPartyId()).isEqualTo(5L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_SALE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.AR.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("40000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_CUSTOMER);
        });
    }

    @Test
    void post_sale_financed_withoutFinanceCompanyRef_leavesLineUntagged() {
        // Historical sales predating this feature won't have financeCompanyRef backfilled.
        Sale sale = buildSale(
                new BigDecimal("100000"), BigDecimal.ZERO, true, new BigDecimal("60000"), new BigDecimal("70000"));
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE, 1L))
                .thenReturn(Optional.empty());

        coaFor(SystemCoaRole.AR);
        coaFor(SystemCoaRole.FINANCE_RECEIVABLE);
        coaFor(SystemCoaRole.COGS);
        coaFor(SystemCoaRole.SALES_REVENUE);
        coaFor(SystemCoaRole.INVENTORY);

        journalService.post(JournalService.REF_SALE, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.FINANCE_RECEIVABLE.name());
            assertThat(line.getPartyType()).isNull();
            assertThat(line.getSourceType()).isNull();
        });
    }

    @Test
    void post_sale_missingChartOfAccount_throwsJournalCoaMissing() {
        Sale sale = buildSale(new BigDecimal("100000"), BigDecimal.ZERO, false, null, BigDecimal.ZERO);
        when(saleRepository.findById(1L)).thenReturn(Optional.of(sale));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE, 1L))
                .thenReturn(Optional.empty());
        when(chartOfAccountRepository.findBySystemRole(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> journalService.post(JournalService.REF_SALE, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("BUS_130"); // JOURNAL_COA_MISSING
    }

    @Test
    void post_purchase_basicCase_postsInventoryAndAP() {
        Purchase purchase = buildPurchase(new BigDecimal("100000"), null);
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_PURCHASE, 1L))
                .thenReturn(Optional.empty());

        coaFor(SystemCoaRole.INVENTORY);
        coaFor(SystemCoaRole.AP);

        journalService.post(JournalService.REF_PURCHASE, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        List<JournalDetail> lines = captor.getValue();

        BigDecimal totalDebit = lines.stream().map(JournalDetail::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalDetail::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit);
        assertThat(totalDebit).isEqualByComparingTo("100000");

        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.INVENTORY.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("100000");
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.AP.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("100000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_VENDOR);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_PURCHASE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void post_purchase_withRcDue_debitsReceivableAndInflatesPayableWithoutTouchingInventory() {
        // 5000 of the 105000 paid to the vendor is a refundable RCD deposit, recoverable once
        // this unit is resold — it must land in RC_DUE_RECEIVABLE, not Inventory, but the vendor
        // was genuinely paid the full 105000 so A/P must reflect that in full.
        Purchase purchase = buildPurchase(new BigDecimal("100000"), new BigDecimal("5000"));
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_PURCHASE, 1L))
                .thenReturn(Optional.empty());

        coaFor(SystemCoaRole.INVENTORY);
        coaFor(SystemCoaRole.RC_DUE_RECEIVABLE);
        coaFor(SystemCoaRole.AP);

        journalService.post(JournalService.REF_PURCHASE, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        List<JournalDetail> lines = captor.getValue();

        BigDecimal totalDebit = lines.stream().map(JournalDetail::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalDetail::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit);
        assertThat(totalDebit).isEqualByComparingTo("105000");

        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.INVENTORY.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("100000"); // rcDue excluded from landed cost
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.RC_DUE_RECEIVABLE.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("5000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_VENDOR);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_PURCHASE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.AP.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("105000"); // full cash paid to vendor
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_VENDOR);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_PURCHASE);
        });
    }

    @Test
    void post_rcDueReceipt_settlesReceivableFromPurchase() {
        Purchase purchase = buildPurchase(new BigDecimal("100000"), new BigDecimal("5000"));

        PaymentAccount cashAccount = new PaymentAccount();
        cashAccount.setId(1L);
        ChartOfAccount cashCoa = new ChartOfAccount();
        cashCoa.setId(50L);
        cashCoa.setLabel("CASH");
        cashAccount.setChartOfAccount(cashCoa);

        RcDueReceipt receipt = new RcDueReceipt();
        receipt.setId(1L);
        receipt.setPurchase(purchase);
        receipt.setAmount(new BigDecimal("5000"));
        receipt.setReceiptDate(LocalDate.of(2026, 2, 1));
        receipt.setPaymentAccount(cashAccount);

        when(rcDueReceiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_RC_DUE_RECEIPT, 1L))
                .thenReturn(Optional.empty());

        coaFor(SystemCoaRole.RC_DUE_RECEIVABLE);

        journalService.post(JournalService.REF_RC_DUE_RECEIPT, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        List<JournalDetail> lines = captor.getValue();

        BigDecimal totalDebit = lines.stream().map(JournalDetail::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream().map(JournalDetail::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebit).isEqualByComparingTo(totalCredit);
        assertThat(totalDebit).isEqualByComparingTo("5000");

        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount()).isEqualTo(cashCoa);
            assertThat(line.getDebitAmount()).isEqualByComparingTo("5000");
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.RC_DUE_RECEIVABLE.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("5000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_VENDOR);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_PURCHASE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }


    // ─────────────────────────────────────────────────────────────────────────
    //  Party/source tagging coverage for the remaining handlers (Part 3) - each
    //  asserts the AR/AP-relevant line traces back to the correct party AND the
    //  correct originating Sale/Purchase, per the plan's per-handler mapping.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void post_salePayment_nonFinance_tagsCustomerAndSale() {
        Sale sale = buildSale(new BigDecimal("100000"), BigDecimal.ZERO, false, null, BigDecimal.ZERO);
        PaymentAccount cashAccount = new PaymentAccount();
        cashAccount.setId(1L);
        ChartOfAccount cashCoa = new ChartOfAccount();
        cashCoa.setId(60L);
        cashAccount.setChartOfAccount(cashCoa);

        com.triasoft.garage.entity.SalePayment payment = new com.triasoft.garage.entity.SalePayment();
        payment.setId(1L);
        payment.setSale(sale);
        payment.setAmount(new BigDecimal("40000"));
        payment.setPaymentDate(LocalDate.of(2026, 1, 15));
        payment.setPaymentAccount(cashAccount);

        when(salePaymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE_PAYMENT, 1L))
                .thenReturn(Optional.empty());
        coaFor(SystemCoaRole.AR);

        journalService.post(JournalService.REF_SALE_PAYMENT, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.AR.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("40000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_CUSTOMER);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_SALE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void post_salePayment_financeDisbursement_tagsFinanceCompanyAndSale() {
        Sale sale = buildSale(new BigDecimal("100000"), BigDecimal.ZERO, true, new BigDecimal("60000"), BigDecimal.ZERO);
        com.triasoft.garage.entity.FinanceCompany financeCompany = new com.triasoft.garage.entity.FinanceCompany();
        financeCompany.setId(5L);
        financeCompany.setName("Bajaj Finance");
        sale.setFinanceCompanyRef(financeCompany);

        PaymentAccount cashAccount = new PaymentAccount();
        cashAccount.setId(1L);
        ChartOfAccount cashCoa = new ChartOfAccount();
        cashCoa.setId(60L);
        cashAccount.setChartOfAccount(cashCoa);

        com.triasoft.garage.entity.SalePayment payment = new com.triasoft.garage.entity.SalePayment();
        payment.setId(1L);
        payment.setSale(sale);
        payment.setAmount(new BigDecimal("60000"));
        payment.setPaymentDate(LocalDate.of(2026, 1, 20));
        payment.setPaymentAccount(cashAccount);
        payment.setPayerType(com.triasoft.garage.constants.PayerTypeEnum.FINANCE);

        when(salePaymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE_PAYMENT, 1L))
                .thenReturn(Optional.empty());
        coaFor(SystemCoaRole.FINANCE_RECEIVABLE);

        journalService.post(JournalService.REF_SALE_PAYMENT, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.FINANCE_RECEIVABLE.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("60000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_FINANCE);
            assertThat(line.getPartyId()).isEqualTo(5L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_SALE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void post_saleReturn_none_cancelsArAndTagsRefundPayableToSale() {
        // Fully unpaid sale, no exchange: outstandingAr = saleRate, refundPayable = 0 - 0 = 0
        // (customerPaid is 0) -> only the AR-cancel line carries a party/source tag here.
        Sale sale = buildSale(new BigDecimal("100000"), BigDecimal.ZERO, false, null, new BigDecimal("70000"));
        com.triasoft.garage.entity.SaleReturn sr = new com.triasoft.garage.entity.SaleReturn();
        sr.setId(1L);
        sr.setSale(sale);
        sr.setReturnDate(LocalDate.of(2026, 2, 1));
        sr.setCustomerPaidAmount(BigDecimal.ZERO);
        sr.setExchangeHandling(com.triasoft.garage.constants.ExchangeHandlingEnum.NONE);

        when(saleReturnRepository.findById(1L)).thenReturn(Optional.of(sr));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE_RETURN, 1L))
                .thenReturn(Optional.empty());
        coaFor(SystemCoaRole.SALES_REVENUE);
        coaFor(SystemCoaRole.INVENTORY);
        coaFor(SystemCoaRole.COGS);
        coaFor(SystemCoaRole.AR);

        journalService.post(JournalService.REF_SALE_RETURN, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.AR.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("100000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_CUSTOMER);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_SALE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void post_saleReturnRefund_tagsCustomerAndOriginalSale() {
        Sale sale = buildSale(new BigDecimal("100000"), BigDecimal.ZERO, false, null, BigDecimal.ZERO);
        com.triasoft.garage.entity.SaleReturn sr = new com.triasoft.garage.entity.SaleReturn();
        sr.setId(1L);
        sr.setSale(sale);

        PaymentAccount cashAccount = new PaymentAccount();
        cashAccount.setId(1L);
        ChartOfAccount cashCoa = new ChartOfAccount();
        cashCoa.setId(61L);
        cashAccount.setChartOfAccount(cashCoa);

        com.triasoft.garage.entity.SaleRefundPayment refund = new com.triasoft.garage.entity.SaleRefundPayment();
        refund.setId(1L);
        refund.setSaleReturn(sr);
        refund.setAmount(new BigDecimal("15000"));
        refund.setPaymentDate(LocalDate.of(2026, 2, 5));
        refund.setPaymentAccount(cashAccount);

        when(saleRefundPaymentRepository.findById(1L)).thenReturn(Optional.of(refund));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE_RETURN_REFUND, 1L))
                .thenReturn(Optional.empty());
        coaFor(SystemCoaRole.CUSTOMER_REFUND_PAYABLE);

        journalService.post(JournalService.REF_SALE_RETURN_REFUND, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.CUSTOMER_REFUND_PAYABLE.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("15000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_CUSTOMER);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_SALE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void post_purchasePayment_nonExchange_tagsVendorAndPurchase() {
        Purchase purchase = buildPurchase(new BigDecimal("100000"), null);
        PaymentAccount cashAccount = new PaymentAccount();
        cashAccount.setId(1L);
        ChartOfAccount cashCoa = new ChartOfAccount();
        cashCoa.setId(62L);
        cashAccount.setChartOfAccount(cashCoa);

        com.triasoft.garage.entity.PurchasePayment payment = new com.triasoft.garage.entity.PurchasePayment();
        payment.setId(1L);
        payment.setPurchase(purchase);
        payment.setAmount(new BigDecimal("50000"));
        payment.setPaymentDate(LocalDate.of(2026, 1, 20));
        payment.setPaymentAccount(cashAccount);

        when(purchasePaymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_PURCHASE_PAYMENT, 1L))
                .thenReturn(Optional.empty());
        when(inventoryRepository.findByPurchaseOrderDetailPurchaseId(1L)).thenReturn(Optional.empty());
        coaFor(SystemCoaRole.AP);

        journalService.post(JournalService.REF_PURCHASE_PAYMENT, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.AP.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("50000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_VENDOR);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_PURCHASE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void post_purchasePayment_exchange_tagsCustomerAndOriginalSaleNotPurchase() {
        // Clearing a CUSTOMER_SETTLEMENT_PAYABLE via a purchase payment (settling an exchange
        // vehicle) must trace back to the SALE that created the liability, not this purchase.
        Purchase purchase = buildPurchase(new BigDecimal("80000"), null);
        purchase.setId(2L);

        Sale originatingSale = buildSale(new BigDecimal("50000"), new BigDecimal("60000"), false, null, BigDecimal.ZERO);
        originatingSale.setId(9L);
        Inventory exchangeInv = new Inventory();
        exchangeInv.setSourceSaleId(9L);

        PaymentAccount cashAccount = new PaymentAccount();
        cashAccount.setId(1L);
        ChartOfAccount cashCoa = new ChartOfAccount();
        cashCoa.setId(63L);
        cashAccount.setChartOfAccount(cashCoa);

        com.triasoft.garage.entity.PurchasePayment payment = new com.triasoft.garage.entity.PurchasePayment();
        payment.setId(2L);
        payment.setPurchase(purchase);
        payment.setAmount(new BigDecimal("10000"));
        payment.setPaymentDate(LocalDate.of(2026, 1, 25));
        payment.setPaymentAccount(cashAccount);

        when(purchasePaymentRepository.findById(2L)).thenReturn(Optional.of(payment));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_PURCHASE_PAYMENT, 2L))
                .thenReturn(Optional.empty());
        when(inventoryRepository.findByPurchaseOrderDetailPurchaseId(2L)).thenReturn(Optional.of(exchangeInv));
        when(saleRepository.findById(9L)).thenReturn(Optional.of(originatingSale));
        coaFor(SystemCoaRole.CUSTOMER_SETTLEMENT_PAYABLE);

        journalService.post(JournalService.REF_PURCHASE_PAYMENT, 2L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.CUSTOMER_SETTLEMENT_PAYABLE.name());
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_CUSTOMER);
            assertThat(line.getPartyId()).isEqualTo(1L); // originatingSale's customer id (buildSale fixture)
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_SALE);
            assertThat(line.getSourceId()).isEqualTo(9L); // the SALE, not the purchase (2L)
        });
    }

    @Test
    void post_purchaseReturn_tagsVendorAndPurchaseOnApAndReceivableLines() {
        Purchase purchase = buildPurchase(new BigDecimal("100000"), null);
        Inventory inv = new Inventory();
        inv.setUin("UIN-1");

        com.triasoft.garage.entity.PurchaseReturn pr = new com.triasoft.garage.entity.PurchaseReturn();
        pr.setId(1L);
        pr.setPurchase(purchase);
        pr.setInventory(inv);
        pr.setReturnDate(LocalDate.of(2026, 2, 10));
        pr.setReturnAmount(new BigDecimal("100000"));
        pr.setInventoryLandedCost(new BigDecimal("100000"));

        when(purchaseReturnRepository.findById(1L)).thenReturn(Optional.of(pr));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_PURCHASE_RETURN, 1L))
                .thenReturn(Optional.empty());
        when(expenseRepository.findByPurchaseId(1L)).thenReturn(List.of());
        when(purchasePaymentRepository.sumAmountByPurchaseId(1L)).thenReturn(BigDecimal.ZERO);
        coaFor(SystemCoaRole.AP);
        coaFor(SystemCoaRole.INVENTORY);

        journalService.post(JournalService.REF_PURCHASE_RETURN, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.AP.name());
            assertThat(line.getDebitAmount()).isEqualByComparingTo("100000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_VENDOR);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_PURCHASE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void post_purchaseReturnReceipt_tagsVendorAndPurchase() {
        Purchase purchase = buildPurchase(new BigDecimal("100000"), null);
        com.triasoft.garage.entity.PurchaseReturn pr = new com.triasoft.garage.entity.PurchaseReturn();
        pr.setId(1L);
        pr.setPurchase(purchase);

        PaymentAccount cashAccount = new PaymentAccount();
        cashAccount.setId(1L);
        ChartOfAccount cashCoa = new ChartOfAccount();
        cashCoa.setId(64L);
        cashAccount.setChartOfAccount(cashCoa);

        com.triasoft.garage.entity.PurchaseReturnReceipt receipt = new com.triasoft.garage.entity.PurchaseReturnReceipt();
        receipt.setId(1L);
        receipt.setPurchaseReturn(pr);
        receipt.setAmount(new BigDecimal("20000"));
        receipt.setPaymentDate(LocalDate.of(2026, 2, 12));
        receipt.setPaymentAccount(cashAccount);

        when(purchaseReturnReceiptRepository.findById(1L)).thenReturn(Optional.of(receipt));
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_PURCHASE_RETURN_RECEIPT, 1L))
                .thenReturn(Optional.empty());
        coaFor(SystemCoaRole.VENDOR_REFUND_RECEIVABLE);

        journalService.post(JournalService.REF_PURCHASE_RETURN_RECEIPT, 1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.VENDOR_REFUND_RECEIVABLE.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("20000");
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_VENDOR);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_PURCHASE);
            assertThat(line.getSourceId()).isEqualTo(1L);
        });
    }

    @Test
    void postExchangeBuybackPurchase_tagsCustomerAndOriginalSaleNotBuybackPurchase() {
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_PURCHASE, 5L))
                .thenReturn(Optional.empty());
        coaFor(SystemCoaRole.INVENTORY);
        coaFor(SystemCoaRole.CUSTOMER_REFUND_PAYABLE);

        journalService.postExchangeBuybackPurchase(5L, new BigDecimal("60000"),
                LocalDate.of(2026, 2, 15), "John Doe", 1L, 9L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.CUSTOMER_REFUND_PAYABLE.name());
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_CUSTOMER);
            assertThat(line.getPartyId()).isEqualTo(1L);
            assertThat(line.getSourceType()).isEqualTo(JournalService.SOURCE_SALE);
            assertThat(line.getSourceId()).isEqualTo(9L); // the sale, not the buyback purchase (5L)
        });
    }

    @Test
    void reverse_noActiveJournal_isNoOp() {
        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE, 99L))
                .thenReturn(Optional.empty());

        journalService.reverse(JournalService.REF_SALE, 99L);

        verify(journalRepository, never()).save(any());
        verifyNoInteractions(journalDetailRepository);
    }

    @Test
    void reverse_swapsDebitAndCreditAndMarksOriginalReversed() {
        ChartOfAccount arAccount = new ChartOfAccount();
        arAccount.setId(1L);
        ChartOfAccount revenueAccount = new ChartOfAccount();
        revenueAccount.setId(2L);

        Journal original = new Journal();
        original.setId(5L);
        original.setJournalDate(LocalDate.of(2026, 1, 10));
        original.setReferenceType(JournalService.REF_SALE);
        original.setReferenceId(1L);
        original.setDescription("Sale INV-1 — John Doe");
        original.setStatus(JournalStatusEnum.POSTED);

        JournalDetail debitLine = new JournalDetail();
        debitLine.setAccount(arAccount);
        debitLine.setDebitAmount(new BigDecimal("100"));
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setPartyType(JournalService.PARTY_CUSTOMER);
        debitLine.setPartyId(1L);

        JournalDetail creditLine = new JournalDetail();
        creditLine.setAccount(revenueAccount);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(new BigDecimal("100"));

        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE, 1L))
                .thenReturn(Optional.of(original));
        when(journalDetailRepository.findByJournalId(5L)).thenReturn(List.of(debitLine, creditLine));

        journalService.reverse(JournalService.REF_SALE, 1L);

        ArgumentCaptor<Journal> journalCaptor = ArgumentCaptor.forClass(Journal.class);
        verify(journalRepository, times(2)).save(journalCaptor.capture()); // reversal + original (status update)
        Journal reversal = journalCaptor.getAllValues().stream()
                .filter(j -> j != original).findFirst().orElseThrow();
        assertThat(reversal.getReversalOf()).isEqualTo(original);
        assertThat(reversal.getJournalDate()).isEqualTo(original.getJournalDate());
        assertThat(reversal.getReferenceType()).isEqualTo(JournalService.REF_SALE);
        assertThat(reversal.getReferenceId()).isEqualTo(1L);
        assertThat(reversal.getDescription()).startsWith("Reversal of:");

        assertThat(original.getStatus()).isEqualTo(JournalStatusEnum.REVERSED);

        ArgumentCaptor<JournalDetail> lineCaptor = ArgumentCaptor.forClass(JournalDetail.class);
        verify(journalDetailRepository, times(2)).save(lineCaptor.capture());
        List<JournalDetail> savedLines = lineCaptor.getAllValues();

        assertThat(savedLines).anySatisfy(line -> {
            assertThat(line.getAccount()).isEqualTo(arAccount);
            assertThat(line.getDebitAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(line.getCreditAmount()).isEqualByComparingTo("100"); // swapped from debit
            // Party tag carries over onto the reversal line too.
            assertThat(line.getPartyType()).isEqualTo(JournalService.PARTY_CUSTOMER);
            assertThat(line.getPartyId()).isEqualTo(1L);
        });
        assertThat(savedLines).anySatisfy(line -> {
            assertThat(line.getAccount()).isEqualTo(revenueAccount);
            assertThat(line.getDebitAmount()).isEqualByComparingTo("100"); // swapped from credit
            assertThat(line.getCreditAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void reverseOnDate_usesSuppliedDateInsteadOfOriginalJournalDate() {
        Journal original = new Journal();
        original.setId(7L);
        original.setJournalDate(LocalDate.of(2026, 1, 10));
        original.setReferenceType(JournalService.REF_SALE_PAYMENT);
        original.setReferenceId(2L);
        original.setDescription("Customer payment");
        original.setStatus(JournalStatusEnum.POSTED);

        when(journalRepository.findActiveByReferenceTypeAndReferenceId(JournalService.REF_SALE_PAYMENT, 2L))
                .thenReturn(Optional.of(original));
        when(journalDetailRepository.findByJournalId(7L)).thenReturn(List.of());

        LocalDate cancellationDate = LocalDate.of(2026, 2, 1);
        journalService.reverseOnDate(JournalService.REF_SALE_PAYMENT, 2L, cancellationDate);

        ArgumentCaptor<Journal> journalCaptor = ArgumentCaptor.forClass(Journal.class);
        verify(journalRepository, times(2)).save(journalCaptor.capture());
        Journal reversal = journalCaptor.getAllValues().stream()
                .filter(j -> j != original).findFirst().orElseThrow();
        assertThat(reversal.getJournalDate()).isEqualTo(cancellationDate);
    }


    @Test
    void createManual_fewerThanTwoLines_throws() {
        JournalRq rq = new JournalRq(LocalDate.now(), "desc", List.of(new JournalLineRq(1L, BigDecimal.TEN, BigDecimal.ZERO, null)));

        assertThatThrownBy(() -> journalService.createManual(rq))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("JNL_420");
    }

    @Test
    void createManual_lineWithBothDebitAndCredit_throws() {
        JournalRq rq = new JournalRq(LocalDate.now(), "desc", List.of(
                new JournalLineRq(1L, BigDecimal.TEN, BigDecimal.TEN, null),
                new JournalLineRq(2L, BigDecimal.ZERO, BigDecimal.TEN, null)));

        assertThatThrownBy(() -> journalService.createManual(rq))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("JNL_421");
    }

    @Test
    void createManual_lineWithNeitherDebitNorCredit_throws() {
        JournalRq rq = new JournalRq(LocalDate.now(), "desc", List.of(
                new JournalLineRq(1L, BigDecimal.ZERO, BigDecimal.ZERO, null),
                new JournalLineRq(2L, BigDecimal.ZERO, BigDecimal.TEN, null)));

        assertThatThrownBy(() -> journalService.createManual(rq))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("JNL_422");
    }

    @Test
    void createManual_missingAccountId_throws() {
        JournalRq rq = new JournalRq(LocalDate.now(), "desc", List.of(
                new JournalLineRq(null, BigDecimal.TEN, BigDecimal.ZERO, null),
                new JournalLineRq(2L, BigDecimal.ZERO, BigDecimal.TEN, null)));

        assertThatThrownBy(() -> journalService.createManual(rq))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("JNL_423");
    }

    @Test
    void createManual_unbalancedLines_throwsJournalNotBalanced() {
        JournalRq rq = new JournalRq(LocalDate.now(), "desc", List.of(
                new JournalLineRq(1L, new BigDecimal("100"), BigDecimal.ZERO, null),
                new JournalLineRq(2L, BigDecimal.ZERO, new BigDecimal("90"), null)));

        assertThatThrownBy(() -> journalService.createManual(rq))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo("BUS_131"); // JOURNAL_NOT_BALANCED
    }

    @Test
    void createManual_balancedLines_postsJournalAndSelfReferencesReferenceId() {
        ChartOfAccount cash = new ChartOfAccount();
        cash.setId(1L);
        ChartOfAccount capital = new ChartOfAccount();
        capital.setId(2L);
        when(chartOfAccountRepository.findById(1L)).thenReturn(Optional.of(cash));
        when(chartOfAccountRepository.findById(2L)).thenReturn(Optional.of(capital));

        JournalRq rq = new JournalRq(LocalDate.of(2026, 3, 1), "Owner capital injection", List.of(
                new JournalLineRq(1L, new BigDecimal("5000"), BigDecimal.ZERO, "Cash in"),
                new JournalLineRq(2L, BigDecimal.ZERO, new BigDecimal("5000"), "Capital")));

        Journal result = journalService.createManual(rq);

        assertThat(result.getReferenceType()).isEqualTo(JournalService.REF_MANUAL_JOURNAL);
        assertThat(result.getReferenceId()).isEqualTo(result.getId());
        assertThat(result.getJournalDate()).isEqualTo(LocalDate.of(2026, 3, 1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JournalDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalDetailRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }
}
