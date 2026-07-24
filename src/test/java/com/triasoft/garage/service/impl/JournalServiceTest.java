package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.JournalStatusEnum;
import com.triasoft.garage.constants.SystemCoaRole;
import com.triasoft.garage.entity.ChartOfAccount;
import com.triasoft.garage.entity.Customer;
import com.triasoft.garage.entity.Journal;
import com.triasoft.garage.entity.JournalDetail;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.journal.JournalLineRq;
import com.triasoft.garage.model.journal.JournalRq;
import com.triasoft.garage.repository.ChartOfAccountRepository;
import com.triasoft.garage.repository.DirectEntryRepository;
import com.triasoft.garage.repository.ExpenseRepository;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.JournalDetailRepository;
import com.triasoft.garage.repository.JournalRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.PurchasePaymentRepository;
import com.triasoft.garage.repository.PurchaseRepository;
import com.triasoft.garage.repository.PurchaseReturnReceiptRepository;
import com.triasoft.garage.repository.PurchaseReturnRepository;
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
 * All repositories are mocked — no DB involved — so these only verify the business
 * math (which accounts get debited/credited, balancing, reversal semantics), not
 * persistence/query correctness.
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

    private JournalService journalService;

    @BeforeEach
    void setUp() {
        journalService = new JournalService(
                journalRepository, journalDetailRepository, chartOfAccountRepository,
                saleRepository, salePaymentRepository, purchaseRepository, purchasePaymentRepository,
                expenseRepository, directEntryRepository, paymentAccountRepository, inventoryRepository,
                saleReturnRepository, saleRefundPaymentRepository, purchaseReturnRepository,
                purchaseReturnReceiptRepository);

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
        });
        assertThat(lines).anySatisfy(line -> {
            assertThat(line.getAccount().getLabel()).isEqualTo(SystemCoaRole.SALES_REVENUE.name());
            assertThat(line.getCreditAmount()).isEqualByComparingTo("100000");
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
