package com.triasoft.garage.service.impl;

import com.triasoft.garage.company.repository.WarehouseBusinessLineRepository;
import com.triasoft.garage.constants.SystemCoaRole;
import com.triasoft.garage.entity.Customer;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.entity.Purchase;
import com.triasoft.garage.entity.PurchaseDetail;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.entity.Vendor;
import com.triasoft.garage.ledger.projection.SourceBalanceRow;
import com.triasoft.garage.ledger.repository.JournalDetailRepository;
import com.triasoft.garage.model.report.RcDueInfo;
import com.triasoft.garage.model.report.RcDueSummaryRs;
import com.triasoft.garage.repository.*;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.security.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Focused coverage for PurchaseService.getRcDueSummary() - the ledger-derived, RC_DUE_RECEIVABLE
 * account-scoped rewrite. Verifies the two behaviors that motivated the rewrite: (1) a purchase
 * with pending RC due shows up even before the unit has a Sale (only the RECEIPT action itself
 * is gated on sale, via requireSold()), and (2) a purchase with more than one inventory unit
 * doesn't crash the per-purchase inventory lookup (Collectors.toMap duplicate-key risk).
 */
@ExtendWith(MockitoExtension.class)
class PurchaseServiceRcDueSummaryTest {

    @Mock private ProductService productService;
    @Mock private AccountService accountService;
    @Mock private ProductRepository productRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private PurchasePaymentRepository purchasePaymentRepository;
    @Mock private PaymentAccountRepository paymentAccountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private VendorRepository vendorRepository;
    @Mock private ReportService reportService;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private WarehouseBusinessLineRepository warehouseBusinessLineRepository;
    @Mock private LookupHelper lookupHelper;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private SaleRepository saleRepository;
    @Mock private RcDueReceiptRepository rcDueReceiptRepository;
    @Mock private JournalService journalService;
    @Mock private JournalDetailRepository journalDetailRepository;
    @Mock private UserProfileRepository userProfileRepository;

    private static final Long COMPANY_ID = 100L;

    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(
                productService, accountService, productRepository, purchaseRepository,
                purchasePaymentRepository, paymentAccountRepository, transactionRepository,
                vendorRepository, reportService, inventoryRepository, warehouseRepository,
                warehouseBusinessLineRepository, lookupHelper, expenseRepository, saleRepository,
                rcDueReceiptRepository, journalService, journalDetailRepository, userProfileRepository);
        TenantContext.set(1L);
        lenient().when(rcDueReceiptRepository.findLastReceiptDatesByPurchaseIds(anyList())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Purchase buildPurchase(Long id, BigDecimal rcDueAmount) {
        Vendor vendor = new Vendor();
        vendor.setId(10L);
        vendor.setName("Acme Motors");
        vendor.setMobile("9999999999");

        Purchase purchase = new Purchase();
        purchase.setId(id);
        purchase.setReferenceNo("PO-" + id);
        purchase.setVendor(vendor);
        purchase.setOrderDate(LocalDate.of(2026, 1, id.intValue()));
        purchase.setRcDueAmount(rcDueAmount);
        return purchase;
    }

    private Inventory buildInventory(Long id, Purchase purchase, String productNo) {
        PurchaseDetail detail = new PurchaseDetail();
        detail.setPurchase(purchase);

        Inventory inv = new Inventory();
        inv.setId(id);
        inv.setPurchaseOrderDetail(detail);
        inv.setProductNo(productNo);
        return inv;
    }

    @Test
    void getRcDueSummary_purchaseWithoutSale_stillAppearsWithNullSaleFields() {
        Purchase soldPurchase = buildPurchase(1L, new BigDecimal("5000"));
        Purchase unsoldPurchase = buildPurchase(2L, new BigDecimal("3000"));

        SourceBalanceRow row1 = mockRow(1L, new BigDecimal("5000"), BigDecimal.ZERO);
        SourceBalanceRow row2 = mockRow(2L, new BigDecimal("3000"), BigDecimal.ZERO);
        when(journalDetailRepository.getOpenSourceBalancesByRole(1L, COMPANY_ID, JournalService.SOURCE_PURCHASE, SystemCoaRole.RC_DUE_RECEIVABLE.name()))
                .thenReturn(List.of(row1, row2));
        when(purchaseRepository.findAllById(anyList())).thenReturn(List.of(soldPurchase, unsoldPurchase));

        Inventory soldInventory = buildInventory(100L, soldPurchase, "KA-01-AB-1234");
        Inventory unsoldInventory = buildInventory(101L, unsoldPurchase, "KA-01-CD-5678");
        when(inventoryRepository.findByPurchaseOrderDetailPurchaseIdIn(anyList()))
                .thenReturn(List.of(soldInventory, unsoldInventory));

        Customer customer = new Customer();
        customer.setId(50L);
        customer.setName("John Doe");
        Sale sale = new Sale();
        sale.setId(500L);
        sale.setInvoiceNo("SO-500");
        sale.setSaleDate(LocalDate.of(2026, 2, 1));
        sale.setInventory(soldInventory);
        sale.setCustomer(customer);
        // Only the SOLD unit has a sale - unsoldInventory has none.
        when(saleRepository.findByInventoryIdIn(anyList())).thenReturn(List.of(sale));

        RcDueSummaryRs result = purchaseService.getRcDueSummary(COMPANY_ID);

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getTotalPendingAmount()).isEqualByComparingTo("8000");

        RcDueInfo soldItem = result.getItems().stream().filter(i -> i.getPurchaseId().equals(1L)).findFirst().orElseThrow();
        assertThat(soldItem.getSaleId()).isEqualTo(500L);
        assertThat(soldItem.getInvoiceNo()).isEqualTo("SO-500");
        assertThat(soldItem.getVehicleNo()).isEqualTo("KA-01-AB-1234");
        assertThat(soldItem.getPendingAmount()).isEqualByComparingTo("5000");

        RcDueInfo unsoldItem = result.getItems().stream().filter(i -> i.getPurchaseId().equals(2L)).findFirst().orElseThrow();
        // The whole point: unsold units still show up, with sale fields null rather than
        // being excluded from the list entirely (the old findPendingRcDues() INNER JOINed
        // through Sale, so this purchase would never have appeared at all).
        assertThat(unsoldItem.getSaleId()).isNull();
        assertThat(unsoldItem.getInvoiceNo()).isNull();
        assertThat(unsoldItem.getSaleDate()).isNull();
        assertThat(unsoldItem.getVehicleNo()).isEqualTo("KA-01-CD-5678");
        assertThat(unsoldItem.getReferenceNo()).isEqualTo("PO-2");
        assertThat(unsoldItem.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(unsoldItem.getPendingAmount()).isEqualByComparingTo("3000");
        assertThat(unsoldItem.getVendorName()).isEqualTo("Acme Motors");
    }

    @Test
    void getRcDueSummary_purchaseWithMultipleInventoryUnits_doesNotThrow() {
        Purchase purchase = buildPurchase(1L, new BigDecimal("5000"));
        SourceBalanceRow row = mockRow(1L, new BigDecimal("5000"), BigDecimal.ZERO);
        when(journalDetailRepository.getOpenSourceBalancesByRole(1L, COMPANY_ID, JournalService.SOURCE_PURCHASE, SystemCoaRole.RC_DUE_RECEIVABLE.name()))
                .thenReturn(List.of(row));
        when(purchaseRepository.findAllById(anyList())).thenReturn(List.of(purchase));

        // Two inventory units under the SAME purchase - Collectors.toMap without a merge
        // function would throw IllegalStateException here.
        Inventory inv1 = buildInventory(100L, purchase, "KA-01-AB-1234");
        Inventory inv2 = buildInventory(101L, purchase, "KA-01-EF-9999");
        when(inventoryRepository.findByPurchaseOrderDetailPurchaseIdIn(anyList())).thenReturn(List.of(inv1, inv2));
        when(saleRepository.findByInventoryIdIn(anyList())).thenReturn(List.of());

        RcDueSummaryRs result = purchaseService.getRcDueSummary(COMPANY_ID);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getPurchaseId()).isEqualTo(1L);
    }

    private SourceBalanceRow mockRow(Long sourceId, BigDecimal debit, BigDecimal credit) {
        return new SourceBalanceRow() {
            public Long getSourceId() { return sourceId; }
            public BigDecimal getDebit() { return debit; }
            public BigDecimal getCredit() { return credit; }
        };
    }
}
