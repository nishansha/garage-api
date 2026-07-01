package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.*;
import com.triasoft.garage.model.report.PayableInfo;
import com.triasoft.garage.model.report.PayablesSummaryRs;
import com.triasoft.garage.projection.*;
import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.dto.PurchasePaymentDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.*;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.product.ProductRq;
import com.triasoft.garage.model.purchase.PurchasePaymentRq;
import com.triasoft.garage.model.purchase.PurchaseRq;
import com.triasoft.garage.model.purchase.PurchaseRs;
import com.triasoft.garage.model.purchase.PurchaseSummaryRs;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.Transaction;
import com.triasoft.garage.repository.ExpenseRepository;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.SaleRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.ProductRepository;
import com.triasoft.garage.repository.PurchasePaymentRepository;
import com.triasoft.garage.repository.PurchaseRepository;
import com.triasoft.garage.repository.TransactionRepository;
import com.triasoft.garage.repository.VendorRepository;
import com.triasoft.garage.util.CommonUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ProductService productService;
    private final AccountService accountService;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;
    private final VendorRepository vendorRepository;
    private final InventoryRepository inventoryRepository;
    private final LookupHelper lookupHelper;
    private final ExpenseRepository expenseRepository;
    private final SaleRepository saleRepository;
    private final JournalService journalService;


    public PurchaseRs getAll(Pageable pageable, UserDTO user) {
        Page<PurchaseListProjection> purchasePage = purchaseRepository.findAllForList(pageable);
        List<PurchaseListProjection> content = purchasePage.getContent();
        List<Long> ids = content.stream().map(PurchaseListProjection::getId).toList();
        List<PurchaseInventoryStatusProjection> inventoryStatuses = ids.isEmpty()
                ? List.of() : inventoryRepository.findStatusByPurchaseIdIn(ids);
        Map<Long, Boolean> soldMap = buildSoldMap(inventoryStatuses);
        Map<Long, Boolean> returnedMap = buildReturnedMap(inventoryStatuses);
        Map<Long, Boolean> exchangeMap = buildExchangeMap(inventoryStatuses);
        Map<Long, BigDecimal> saleRateMap = buildSaleRateMap(inventoryStatuses);
        Map<Long, BigDecimal> paidMap = getPaidAmountMap(ids);
        Map<Long, Boolean> editabilityMap = buildEditabilityMap(ids, soldMap);
        List<PurchaseDTO> purchases = content.stream()
                .map(p -> convertListProjectionToDTO(p, soldMap.getOrDefault(p.getId(), false),
                        returnedMap.getOrDefault(p.getId(), false),
                        paidMap.getOrDefault(p.getId(), BigDecimal.ZERO),
                        editabilityMap.getOrDefault(p.getId(), true),
                        exchangeMap.getOrDefault(p.getId(), false),
                        saleRateMap.get(p.getId())))
                .toList();
        PurchaseRs purchaseRs = PurchaseRs.builder().purchases(purchases).build();
        purchaseRs.setTotalPages(purchasePage.getTotalPages());
        purchaseRs.setTotalElements(purchasePage.getTotalElements());
        return purchaseRs;
    }

    private PurchaseDTO convertToDTO(Purchase purchase, Inventory inventory, BigDecimal paidAmount) {
        if (CollectionUtils.isEmpty(purchase.getPurchaseDetails())) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND);
        }
        PurchaseDetail purchaseDetail = purchase.getPurchaseDetails().get(0);
        Product product = purchaseDetail.getProduct();
        BigDecimal unitCost = purchaseDetail.getUnitCost();
        boolean isExchange = inventory != null && inventory.getSourceSaleId() != null;
        BigDecimal settlementAmount = isExchange ? unitCost : null;
        BigDecimal paidByOffset = isExchange ? resolvePaidByOffset(inventory.getSourceSaleId(), unitCost) : null;
        BigDecimal paidByCash = isExchange ? paidAmount : null;
        BigDecimal effectivePaid = isExchange ? clampZero(safe(paidByOffset).add(safe(paidByCash))) : paidAmount;
        BigDecimal pending = isExchange
                ? clampZero(unitCost.subtract(effectivePaid))
                : clampZero(unitCost.subtract(paidAmount));
        StatusEnum status = isExchange
                ? deriveSettlementStatus(unitCost, safe(paidByOffset), safe(paidByCash))
                : derivePaymentStatus(paidAmount, unitCost);
        LookupMaster color = inventory != null ? inventory.getColor() : null;
        return PurchaseDTO.builder()
                .id(purchase.getId())
                .date(purchase.getOrderDate())
                .deliveredDate(purchase.getDeliveredDate())
                .code(purchaseDetail.getUuid())
                .vehicleNo(purchaseDetail.getProductNo())
                .brandName(product.getBrand() != null ? product.getBrand().getDescription() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .modelName(product.getModel() != null ? product.getModel().getDescription() : null)
                .modelId(product.getModel() != null ? product.getModel().getId() : null)
                .variantName(product.getVarient() != null ? product.getVarient().getDescription() : null)
                .variantId(product.getVarient() != null ? product.getVarient().getId() : null)
                .segmentId(product.getSegment() != null ? product.getSegment().getId() : null)
                .segmentName(product.getSegment() != null ? product.getSegment().getDescription() : null)
                .purchaseRate(purchaseDetail.getUnitCost())
                .totalCost(purchase.getTotalAmount())
                .paidAmount(effectivePaid)
                .pendingAmount(pending)
                .paymentStatus(status)
                .isExchange(isExchange)
                .sourceSaleId(inventory != null ? inventory.getSourceSaleId() : null)
                .settlementAmount(settlementAmount)
                .paidByOffset(paidByOffset)
                .paidByCash(paidByCash)
                .makeYear(inventory != null ? inventory.getMakeYear() : null)
                .colorName(color != null ? color.getDescription() : null)
                .colorId(color != null ? color.getId() : null)
                .warehouseId(inventory != null ? inventory.getWarehouseId() : null)
                .odometer(purchaseDetail.getOdometer())
                .pickupLocation(purchase.getPickupLocation())
                .pickupStaffId(purchase.getPickupStaffId())
                .notes(purchase.getNotes())
                .ownerName(purchase.getVendor().getName())
                .ownerMobileNo(purchase.getVendor().getMobile())
                .ownerShipSerialNo(purchaseDetail.getOwnershipSerialNo())
                .isSold(inventory != null && StatusEnum.SOLD.equals(inventory.getStatus()))
                .isReturned(inventory != null && StatusEnum.RETURNED_TO_VENDOR.equals(inventory.getStatus()))
                .build();
    }

    private BigDecimal computeRemainingPayable(Purchase purchase, BigDecimal alreadyPaidByCash) {
        Optional<Inventory> invOpt = inventoryRepository.findByPurchaseOrderDetailPurchaseId(purchase.getId());
        if (invOpt.isPresent() && invOpt.get().getSourceSaleId() != null) {
            BigDecimal unitCost = purchase.getPurchaseDetails().get(0).getUnitCost();
            BigDecimal saleOffset = resolvePaidByOffset(invOpt.get().getSourceSaleId(), unitCost);
            return unitCost.subtract(saleOffset).subtract(alreadyPaidByCash);
        }
        return purchase.getTotalAmount().subtract(alreadyPaidByCash);
    }

    private BigDecimal resolvePaidByOffset(Long sourceSaleId, BigDecimal settlementAmount) {
        if (sourceSaleId == null) return BigDecimal.ZERO;
        return saleRepository.findById(sourceSaleId)
                .map(Sale::getSaleRate)
                .map(rate -> rate.min(settlementAmount))
                .orElse(BigDecimal.ZERO);
    }

    private StatusEnum deriveSettlementStatus(BigDecimal settlementAmount, BigDecimal paidByOffset, BigDecimal paidByCash) {
        BigDecimal totalPaid = paidByOffset.add(paidByCash);
        if (totalPaid.compareTo(settlementAmount) >= 0) return StatusEnum.PAID;
        if (totalPaid.compareTo(BigDecimal.ZERO) > 0) return StatusEnum.PARTIAL;
        return StatusEnum.PENDING;
    }

    private BigDecimal clampZero(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private StatusEnum derivePaymentStatus(BigDecimal paidAmount, BigDecimal totalAmount) {
        if (paidAmount.compareTo(BigDecimal.ZERO) == 0) return StatusEnum.PENDING;
        if (paidAmount.compareTo(totalAmount) >= 0) return StatusEnum.PAID;
        return StatusEnum.PARTIAL;
    }

    private Map<Long, BigDecimal> getPaidAmountMap(List<Long> purchaseIds) {
        if (CollectionUtils.isEmpty(purchaseIds)) return Map.of();
        return purchasePaymentRepository.getTotalPaidByPurchaseIds(purchaseIds).stream()
                .collect(Collectors.toMap(PurchasePaidProjection::getPurchaseId, PurchasePaidProjection::getTotalPaid));
    }

    private Map<Long, Boolean> buildSoldMap(List<PurchaseInventoryStatusProjection> rows) {
        return rows.stream().collect(Collectors.toMap(
                PurchaseInventoryStatusProjection::getPurchaseId,
                p -> StatusEnum.SOLD.equals(p.getStatus()),
                (a, b) -> a
        ));
    }

    private Map<Long, Boolean> buildReturnedMap(List<PurchaseInventoryStatusProjection> rows) {
        return rows.stream().collect(Collectors.toMap(
                PurchaseInventoryStatusProjection::getPurchaseId,
                p -> StatusEnum.RETURNED_TO_VENDOR.equals(p.getStatus()),
                (a, b) -> a
        ));
    }

    private Map<Long, Boolean> buildExchangeMap(List<PurchaseInventoryStatusProjection> rows) {
        return rows.stream().collect(Collectors.toMap(
                PurchaseInventoryStatusProjection::getPurchaseId,
                p -> p.getSourceSaleId() != null,
                (a, b) -> a
        ));
    }

    private Map<Long, BigDecimal> buildSaleRateMap(List<PurchaseInventoryStatusProjection> rows) {
        Map<Long, Long> purchaseToSaleId = rows.stream()
                .filter(r -> r.getSourceSaleId() != null)
                .collect(Collectors.toMap(
                        PurchaseInventoryStatusProjection::getPurchaseId,
                        PurchaseInventoryStatusProjection::getSourceSaleId,
                        (a, b) -> a));
        if (purchaseToSaleId.isEmpty()) return Map.of();
        Map<Long, BigDecimal> rateBySaleId = saleRepository.findAllById(purchaseToSaleId.values()).stream()
                .collect(Collectors.toMap(Sale::getId, Sale::getSaleRate, (a, b) -> a));
        Map<Long, BigDecimal> result = new java.util.HashMap<>();
        purchaseToSaleId.forEach((purchaseId, saleId) -> {
            BigDecimal rate = rateBySaleId.get(saleId);
            if (rate != null) result.put(purchaseId, rate);
        });
        return result;
    }

    private PurchaseDTO convertListProjectionToDTO(PurchaseListProjection p, boolean isSold, boolean isReturned,  BigDecimal paidAmount, boolean isEditable, boolean isExchange, BigDecimal sourceSaleRate) {
        BigDecimal total = p.getPurchaseRate();
        BigDecimal paidByOffset = isExchange && sourceSaleRate != null ? sourceSaleRate.min(total) : null;
        BigDecimal paidByCash = isExchange ? paidAmount : null;
        BigDecimal effectivePaid = isExchange ? clampZero(safe(paidByOffset).add(safe(paidByCash))) : paidAmount;
        BigDecimal pending = isExchange ? clampZero(total.subtract(effectivePaid)) : clampZero(total.subtract(paidAmount));
        StatusEnum status = isExchange
                ? deriveSettlementStatus(total, safe(paidByOffset), safe(paidByCash))
                : derivePaymentStatus(paidAmount, total);
        return PurchaseDTO.builder()
                .id(p.getId())
                .date(p.getDate())
                .code(p.getCode())
                .vehicleNo(p.getVehicleNo())
                .brandName(p.getBrandName())
                .modelName(p.getModelName())
                .variantName(p.getVariantName())
                .purchaseRate(total)
                .paidAmount(effectivePaid)
                .pendingAmount(pending)
                .paymentStatus(status)
                .isSold(isSold)
                .isEditable(isEditable)
                .isReturned(isReturned)
                .isExchange(isExchange)
                .settlementAmount(isExchange ? total : null)
                .paidByOffset(paidByOffset)
                .paidByCash(paidByCash)
                .build();
    }

    private PurchaseDTO convertListProjectionToDTOWithExpenses(PurchaseListProjection p, boolean isSold, BigDecimal paidAmount, BigDecimal totalExpenses, boolean isEditable, boolean isExchange, BigDecimal sourceSaleRate) {
        BigDecimal total = p.getPurchaseRate();
        BigDecimal paidByOffset = isExchange && sourceSaleRate != null ? sourceSaleRate.min(total) : null;
        BigDecimal paidByCash = isExchange ? paidAmount : null;
        BigDecimal effectivePaid = isExchange ? clampZero(safe(paidByOffset).add(safe(paidByCash))) : paidAmount;
        BigDecimal pending = isExchange ? clampZero(total.subtract(effectivePaid)) : clampZero(total.subtract(paidAmount));
        StatusEnum status = isExchange
                ? deriveSettlementStatus(total, safe(paidByOffset), safe(paidByCash))
                : derivePaymentStatus(paidAmount, total);
        return PurchaseDTO.builder()
                .id(p.getId())
                .date(p.getDate())
                .code(p.getCode())
                .vehicleNo(p.getVehicleNo())
                .brandName(p.getBrandName())
                .modelName(p.getModelName())
                .variantName(p.getVariantName())
                .purchaseRate(total)
                .totalExpenses(totalExpenses)
                .paidAmount(effectivePaid)
                .pendingAmount(pending)
                .paymentStatus(status)
                .isSold(isSold)
                .isEditable(isEditable)
                .isExchange(isExchange)
                .settlementAmount(isExchange ? total : null)
                .paidByOffset(paidByOffset)
                .paidByCash(paidByCash)
                .build();
    }

    public PurchaseRs getPurchasesWithExpenses(Pageable pageable, UserDTO user) {
        Page<PurchaseListProjection> purchasePage = purchaseRepository.findAllWithExpenses(pageable);
        List<PurchaseListProjection> content = purchasePage.getContent();
        List<Long> ids = content.stream().map(PurchaseListProjection::getId).toList();
        List<PurchaseInventoryStatusProjection> inventoryStatuses = ids.isEmpty()
                ? List.of() : inventoryRepository.findStatusByPurchaseIdIn(ids);
        Map<Long, Boolean> soldMap = buildSoldMap(inventoryStatuses);
        Map<Long, Boolean> exchangeMap = buildExchangeMap(inventoryStatuses);
        Map<Long, BigDecimal> saleRateMap = buildSaleRateMap(inventoryStatuses);
        Map<Long, BigDecimal> paidMap = getPaidAmountMap(ids);
        Map<Long, BigDecimal> expenseSumMap = getExpenseSumMap(ids);
        Map<Long, Boolean> editabilityMap = buildEditabilityMap(ids, soldMap);
        List<PurchaseDTO> purchases = content.stream()
                .map(p -> convertListProjectionToDTOWithExpenses(
                        p,
                        soldMap.getOrDefault(p.getId(), false),
                        paidMap.getOrDefault(p.getId(), BigDecimal.ZERO),
                        expenseSumMap.getOrDefault(p.getId(), BigDecimal.ZERO),
                        editabilityMap.getOrDefault(p.getId(), true),
                        exchangeMap.getOrDefault(p.getId(), false),
                        saleRateMap.get(p.getId())))
                .toList();
        PurchaseRs purchaseRs = PurchaseRs.builder().purchases(purchases).build();
        purchaseRs.setTotalPages(purchasePage.getTotalPages());
        purchaseRs.setTotalElements(purchasePage.getTotalElements());
        return purchaseRs;
    }

    private Map<Long, BigDecimal> getExpenseSumMap(List<Long> purchaseIds) {
        if (CollectionUtils.isEmpty(purchaseIds)) return Map.of();
        return expenseRepository.getTotalExpensesByPurchaseIds(purchaseIds).stream()
                .collect(Collectors.toMap(PurchaseExpenseSumProjection::getPurchaseId, PurchaseExpenseSumProjection::getTotalExpenses));
    }

    private Map<Long, Boolean> buildEditabilityMap(List<Long> allIds, Map<Long, Boolean> soldMap) {
        Map<Long, Boolean> result = new HashMap<>();
        allIds.forEach(id -> result.put(id, true));
        List<Long> soldIds = allIds.stream().filter(id -> soldMap.getOrDefault(id, false)).toList();
        if (CollectionUtils.isEmpty(soldIds)) return result;
        saleRepository.findEditabilityInfoByPurchaseIds(soldIds).forEach(info -> {
            boolean editable = true;
            if (Boolean.TRUE.equals(info.getExpenseLockEnabled()) && info.getExpenseLockWindow() != null) {
                LocalDate deadline = computeExpenseDeadline(info.getSaleDate(), info.getExpenseLockWindow());
                editable = !LocalDate.now().isAfter(deadline);
            }
            result.put(info.getPurchaseId(), editable);
        });
        return result;
    }

    /**
     * Keeps the active Sale snapshot and SALE journal aligned whenever
     * inventory.landedCost changes on a sold vehicle (post-sale expense
     * add/edit/delete, or purchase rate edit within the lock window).
     *
     * The EXPENSE journal always debits Inventory (capitalised). This method
     * then reverse+reposts the SALE journal on the ORIGINAL sale date so that
     * COGS in the sale month reflects the updated landed cost — no cross-month
     * bleed because JournalService.reverse() now uses original.getJournalDate().
     */
    public void syncSaleAfterLandedCostChange(Inventory inventory) {
        if (!StatusEnum.SOLD.equals(inventory.getStatus()))
            return;
        Sale sale = saleRepository.findByInventoryId(inventory.getId());
        if (sale == null)
            return;
        BigDecimal newLandedCost = inventory.getLandedCost();
        sale.setLandedCostAtSale(newLandedCost);
        sale.setProfitAmount(sale.getSaleRate().subtract(newLandedCost));
        saleRepository.save(sale);
        journalService.reverse(JournalService.REF_SALE, sale.getId());
        journalService.post(JournalService.REF_SALE, sale.getId());
    }

    private boolean computeIsEditableForDetail(Purchase purchase, Inventory inventory) {

        if (Objects.nonNull(inventory) && StatusEnum.RETURNED_TO_VENDOR.equals(inventory.getStatus()))
            return false;

        if (inventory == null || !StatusEnum.SOLD.equals(inventory.getStatus()))
            return true;

        if (CollectionUtils.isEmpty(purchase.getPurchaseDetails())) return true;
        var category = purchase.getPurchaseDetails().get(0).getProduct().getCategory();
        if (category == null || !category.isExpenseLockEnabled() || category.getExpenseLockWindow() == null) return true;
        Sale sale = saleRepository.findByInventoryId(inventory.getId());
        if (sale == null) return true;
        return !LocalDate.now().isAfter(computeExpenseDeadline(sale.getSaleDate(), category.getExpenseLockWindow()));
    }

    private LocalDate computeExpenseDeadline(LocalDate saleDate, ExpenseLockWindow window) {
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

    public PurchaseSummaryRs summary(UserDTO user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfMonth.minusDays(1);

        PurchaseMetrics metrics = purchaseRepository.getPurchaseSummaryMetrics(startOfLastMonth, endOfLastMonth, startOfMonth, today);
        double deltaPercent = CommonUtil.calculateDelta(metrics.getTotalThisMonth(), metrics.getTotalLastMonth());

        return PurchaseSummaryRs.builder().totalThisMonth(metrics.getTotalThisMonth().toString()).monthRate(deltaPercent).todayCount(metrics.getTodayCount()).totalCount(metrics.getMonthCount()).build();
    }

    public PurchaseRs search(FilterRq filterRq, Pageable pageable, UserDTO user) {
        Long brandId = filterRq.getBrandId() != null ? Long.parseLong(filterRq.getBrandId()) : null;
        Long modelId = filterRq.getModelId() != null ? Long.parseLong(filterRq.getModelId()) : null;
        Long variantId = filterRq.getVariantId() != null ? Long.parseLong(filterRq.getVariantId()) : null;
        Page<PurchaseListProjection> purchasePage = purchaseRepository.searchForList(
                filterRq.getFromDate(), filterRq.getToDate(),
                brandId, modelId, variantId,
                filterRq.getVehicleNo(), filterRq.getSearchText(),
                pageable);
        List<PurchaseListProjection> content = purchasePage.getContent();
        List<Long> ids = content.stream().map(PurchaseListProjection::getId).toList();
        List<PurchaseInventoryStatusProjection> inventoryStatuses = ids.isEmpty()
                ? List.of() : inventoryRepository.findStatusByPurchaseIdIn(ids);
        Map<Long, Boolean> soldMap = buildSoldMap(inventoryStatuses);
        Map<Long, Boolean> returnedMap = buildReturnedMap(inventoryStatuses);
        Map<Long, Boolean> exchangeMap = buildExchangeMap(inventoryStatuses);
        Map<Long, BigDecimal> saleRateMap = buildSaleRateMap(inventoryStatuses);
        Map<Long, BigDecimal> paidMap = getPaidAmountMap(ids);
        Map<Long, Boolean> editabilityMap = buildEditabilityMap(ids, soldMap);
        List<PurchaseDTO> purchases = content.stream()
                .map(p -> convertListProjectionToDTO(p, soldMap.getOrDefault(p.getId(), false),
                        returnedMap.getOrDefault(p.getId(), false),
                        paidMap.getOrDefault(p.getId(), BigDecimal.ZERO),
                        editabilityMap.getOrDefault(p.getId(), true),
                        exchangeMap.getOrDefault(p.getId(), false),
                        saleRateMap.get(p.getId())))
                .toList();
        PurchaseRs purchaseRs = PurchaseRs.builder().purchases(purchases).build();
        purchaseRs.setTotalPages(purchasePage.getTotalPages());
        purchaseRs.setTotalElements(purchasePage.getTotalElements());
        return purchaseRs;
    }

    @Transactional
    public PurchaseRs create(PurchaseRq purchaseRq, UserDTO user) {
        PurchaseRs rs = new PurchaseRs();
        Vendor vendor = vendorRepository.findByMobile(purchaseRq.getOwnerMobileNo()).orElseGet(() -> createVendor(purchaseRq, user));
        Product product = findOrCreateProduct(purchaseRq);
        Purchase purchase = new Purchase();
        purchase.setVendor(vendor);
        purchase.setReferenceNo("PO-" + purchaseRepository.getNextReferenceNumber());
        purchase.setOrderDate(purchaseRq.getDate());
        if (purchaseRq.getDeliveredDate() != null) {
            purchase.setStatus(lookupHelper.getStatus(LookupTypeEnum.PURCHASE_STATUS, StatusEnum.RECIEVED));
            purchase.setDeliveredDate(purchaseRq.getDeliveredDate());
        } else {
            purchase.setStatus(lookupHelper.getStatus(LookupTypeEnum.PURCHASE_STATUS, StatusEnum.PENDING));
        }
        purchase.setNotes(purchaseRq.getNotes());
        BigDecimal totalExpenseAmt = createAndGetExpense(purchaseRq, purchase, user);
        PurchaseDetail detail = getPurchaseDetail(purchaseRq, product, purchase);
        purchase.setPurchaseDetails(List.of(detail));
        purchase.setTotalAmount(purchaseRq.getPurchaseRate().add(totalExpenseAmt));
        purchase.setPickupLocation(purchaseRq.getPickupLocation());
        purchase.setPickupStaffId(purchaseRq.getPickupStaffId());
        Purchase savedPurchase = purchaseRepository.save(purchase);
        rs.setId(savedPurchase.getId());
        savedPurchase.getPurchaseExpenses().forEach(e -> createExpenseTransaction(e, savedPurchase.getReferenceNo()));

        // TODO [JOURNAL ENTRY] - Purchase Created
        // Trigger  : after every new purchase is saved.
        // Entry 1 – Vehicle acquired into inventory:
        //   Dr  Vehicle Inventory        (Asset   – Current Assets)  purchaseRate + totalExpenseAmt (landed cost)
        //   Cr  Accounts Payable–Vendor  (Liability)                 purchaseRate + totalExpenseAmt
        // Entry 2 – Per purchase expense (if any expenses exist):
        //   Dr  <ExpenseAccount.name>    (Expense)                   expenseAmount  (per expense line)
        //   Cr  Accounts Payable–Vendor  (Liability)                 expenseAmount
        // Note: Entries should be posted only once the purchase is RECEIVED (deliveredDate set).
        //       If status is PENDING_DELIVERY, consider posting a "Goods in Transit" entry instead,
        //       then reversing it and posting the inventory entry when delivered.
        // Future call: JournalEntryService.postPurchase(savedPurchase, totalExpenseAmt)
        // CoA required: "Vehicle Inventory" (Asset), "Accounts Payable" (Liability),
        //               per-expense CoA accounts (already on ChartOfAccount entity).

        StatusEnum inventoryStatus = purchaseRq.getDeliveredDate() != null ? StatusEnum.AVAILABLE : StatusEnum.PENDING_DELIVERY;
        createInventoryRecord(savedPurchase, detail, product, purchaseRq, totalExpenseAmt, inventoryStatus);
        if (purchaseRq.getSourceSaleId() == null) {
            journalService.post(JournalService.REF_PURCHASE, savedPurchase.getId());
        }
        return rs;
    }

    @Transactional
    public PurchaseRs update(Long purchaseId, PurchaseRq purchaseRq, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(purchaseId).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        boolean isExchange = isExchangePurchase(purchaseId);
        if (!isExchange) {
            journalService.reverse(JournalService.REF_PURCHASE, purchaseId);
        }
        Optional<Inventory> inventoryOpt = inventoryRepository.findByPurchaseOrderDetailPurchaseId(purchaseId);
        if (inventoryOpt.isPresent() && StatusEnum.SOLD.equals(inventoryOpt.get().getStatus())) {
            if (!computeIsEditableForDetail(purchase, inventoryOpt.get())) {
                throw new BusinessException(ErrorCode.Business.PURCHASE_EXPENSE_LOCKED);
            }
        }
        Set<Long> existingExpenseIds = purchase.getPurchaseExpenses().stream()
                .map(Expense::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        updateVendor(purchase, purchaseRq, user);
        Product product = findOrCreateProduct(purchaseRq);
        purchase.setOrderDate(purchaseRq.getDate());
        purchase.setPickupLocation(purchaseRq.getPickupLocation());
        purchase.setPickupStaffId(purchaseRq.getPickupStaffId());
        purchase.setNotes(purchaseRq.getNotes());

        // Bug 10 & 11: status transition — PENDING → RECEIVED when deliveredDate is first provided
        if (purchaseRq.getDeliveredDate() != null && purchase.getDeliveredDate() == null) {
            purchase.setStatus(lookupHelper.getStatus(LookupTypeEnum.PURCHASE_STATUS, StatusEnum.RECIEVED));
            purchase.setDeliveredDate(purchaseRq.getDeliveredDate());
        }

        // Bug 8 & 9: remove expenses not present in the request before processing
        if (!CollectionUtils.isEmpty(purchaseRq.getExpenses())) {
            Set<Long> incomingIds = purchaseRq.getExpenses().stream()
                    .filter(e -> e.getId() != null)
                    .map(ExpenseDTO::getId)
                    .collect(Collectors.toSet());
            purchase.getPurchaseExpenses().stream()
                    .filter(e -> !incomingIds.contains(e.getId()))
                    .forEach(this::reverseExpenseTransaction);
            purchase.getPurchaseExpenses().removeIf(e -> !incomingIds.contains(e.getId()));
        } else {
            purchase.getPurchaseExpenses().forEach(this::reverseExpenseTransaction);
            purchase.getPurchaseExpenses().clear();
        }
        // Flush orphan deletions before adding new expenses — Hibernate processes INSERTs before DELETEs
        // during a single flush, which would violate the (purchase_order_id, expense_account_id) unique constraint.
        entityManager.flush();

        BigDecimal totalExpenseAmt = createAndGetExpense(purchaseRq, purchase, user);
        purchase.setTotalAmount(purchaseRq.getPurchaseRate().add(totalExpenseAmt));
        if (CollectionUtils.isEmpty(purchase.getPurchaseDetails())) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND);
        }
        PurchaseDetail detail = purchase.getPurchaseDetails().get(0);
        detail.setProduct(product);
        detail.setUnitCost(purchaseRq.getPurchaseRate());
        detail.setOwnershipSerialNo(purchaseRq.getOwnerShipSerialNo());
        Purchase savedPurchase = purchaseRepository.save(purchase);
        savedPurchase.getPurchaseExpenses().stream()
                .filter(e -> !existingExpenseIds.contains(e.getId()))
                .forEach(e -> createExpenseTransaction(e, savedPurchase.getReferenceNo()));

        // TODO [JOURNAL ENTRY] - Purchase Updated
        // Trigger  : after a purchase amount or expense is changed.
        // Strategy : reverse the original journal entry, then post a fresh one with updated amounts.
        //   Reversal:  Dr Accounts Payable–Vendor  (Liability)   originalTotalAmount
        //              Cr Vehicle Inventory         (Asset)       originalTotalAmount
        //   New entry: Dr Vehicle Inventory         (Asset)       newTotalAmount
        //              Cr Accounts Payable–Vendor   (Liability)   newTotalAmount
        // Alternative: post a single net-difference adjustment entry if amounts differ by a small delta.
        // Future call: JournalEntryService.reverseByReference("PURCHASE", purchaseId);
        //              JournalEntryService.postPurchase(updatedPurchase, totalExpenseAmt)

        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setProduct(product);
            inventory.setProductNo(purchaseRq.getVehicleNo());
            inventory.setOdometer(parseOdometer(purchaseRq.getOdometer()));
            inventory.setUin(StringUtils.hasLength(purchaseRq.getCode()) ? purchaseRq.getCode() : purchaseRq.getVehicleNo());
            inventory.setMakeYear(purchaseRq.getMakeYear());
            inventory.setColor(Objects.nonNull(purchaseRq.getColorId()) ? lookupHelper.get(purchaseRq.getColorId()) : null);
            inventory.setWarehouseId(purchaseRq.getWarehouseId());
            BigDecimal newLandedCost = purchaseRq.getPurchaseRate().add(totalExpenseAmt);
            inventory.setLandedCost(newLandedCost);
            // PENDING_DELIVERY → AVAILABLE: vehicle has now been received
            if (StatusEnum.PENDING_DELIVERY.equals(inventory.getStatus()) && purchaseRq.getDeliveredDate() != null) {
                inventory.setStatus(StatusEnum.AVAILABLE);
                inventory.setReceivedDate(purchaseRq.getDeliveredDate().atStartOfDay());
            }
            inventoryRepository.save(inventory);
            syncSaleAfterLandedCostChange(inventory);
        }

        if (!isExchange) {
            journalService.post(JournalService.REF_PURCHASE, purchaseId);
        }
        return new PurchaseRs();
    }

    private Expense getPurchaseExpense(ExpenseDTO exDto, Purchase purchase, PaymentAccount paymentAccount, UserDTO user) {
        Expense expense = new Expense();
        expense.setDate(exDto.getDate() != null ? exDto.getDate() : purchase.getOrderDate());
        expense.setAmount(exDto.getAmount());
        expense.setDescription(exDto.getDescription());
        expense.setPurchase(purchase);
        expense.setExpenseAccount(accountService.getOrCreateExpenseAccount(exDto, user));
        expense.setPaymentAccount(paymentAccount);
        return expense;
    }


    private PurchaseDetail getPurchaseDetail(PurchaseRq purchaseRq, Product product, Purchase purchase) {
        PurchaseDetail detail = new PurchaseDetail();
        detail.setUuid(StringUtils.hasLength(purchaseRq.getCode()) ? purchaseRq.getCode() : purchaseRq.getVehicleNo());
        detail.setProduct(product);
        detail.setPurchase(purchase);
        detail.setQuantity(1.0);
        detail.setUnitCost(purchaseRq.getPurchaseRate());
        detail.setProductNo(purchaseRq.getVehicleNo());
        detail.setOdometer(purchaseRq.getOdometer());
        detail.setDiscount(0D);
        detail.setDiscountAmount(BigDecimal.ZERO);
        detail.setTax(0D);
        detail.setTaxAmount(BigDecimal.ZERO);
        detail.setOwnershipSerialNo(purchaseRq.getOwnerShipSerialNo());
        return detail;
    }

    private Vendor createVendor(PurchaseRq purchaseRq, UserDTO user) {
        Vendor newVendor = new Vendor();
        newVendor.setName(purchaseRq.getOwnerName());
        newVendor.setMobile(purchaseRq.getOwnerMobileNo());
        return vendorRepository.save(newVendor);
    }

    private void updateVendor(Purchase purchase, PurchaseRq purchaseRq, UserDTO user) {
        Vendor vendor = vendorRepository.findByMobile(purchaseRq.getOwnerMobileNo()).orElseGet(() -> createVendor(purchaseRq, user));

        if (!vendor.getName().equalsIgnoreCase(purchaseRq.getOwnerName())) {
            vendor.setName(purchaseRq.getOwnerName());
            vendorRepository.save(vendor);
        }
        purchase.setVendor(vendor);
    }


    private BigDecimal createAndGetExpense(PurchaseRq purchaseRq, Purchase purchase, UserDTO user) {
        if (CollectionUtils.isEmpty(purchaseRq.getExpenses())) return BigDecimal.ZERO;

        // Aggregate new expenses (no id) per payment account and validate total upfront
        Map<Long, BigDecimal> newExpenseTotalsByAccount = new HashMap<>();
        for (ExpenseDTO exDto : purchaseRq.getExpenses()) {
            if (exDto.getId() == null && exDto.getPaymentAccountId() != null) {
                newExpenseTotalsByAccount.merge(exDto.getPaymentAccountId(), exDto.getAmount(), BigDecimal::add);
            }
        }
        Map<Long, PaymentAccount> validatedAccounts = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> entry : newExpenseTotalsByAccount.entrySet()) {
            validatedAccounts.put(entry.getKey(), resolveAndValidateAccount(entry.getKey(), entry.getValue()));
        }

        BigDecimal totalExpenseAmt = BigDecimal.ZERO;
        for (ExpenseDTO exDto : purchaseRq.getExpenses()) {
            if (Objects.nonNull(exDto.getId())) {
                Expense expense = purchase.getPurchaseExpenses().stream()
                        .filter(e -> e.getId().equals(exDto.getId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));
                boolean amountChanged = expense.getAmount().compareTo(exDto.getAmount()) != 0;
                Long currentAccountId = expense.getPaymentAccount() != null ? expense.getPaymentAccount().getId() : null;
                boolean accountChanged = exDto.getPaymentAccountId() != null && !exDto.getPaymentAccountId().equals(currentAccountId);
                if ((amountChanged || accountChanged) && expense.getPaymentAccount() != null) {
                    reverseExpenseTransaction(expense);
                }
                expense.setDate(exDto.getDate());
                expense.setAmount(exDto.getAmount());
                expense.setDescription(exDto.getDescription());
                expense.setExpenseAccount(accountService.getOrCreateExpenseAccount(exDto, user));
                if (accountChanged) {
                    PaymentAccount newAccount = resolveAndValidateAccount(exDto.getPaymentAccountId(), exDto.getAmount());
                    expense.setPaymentAccount(newAccount);
                } else if (amountChanged && expense.getPaymentAccount() != null) {
                    resolveAndValidateAccount(expense.getPaymentAccount().getId(), exDto.getAmount());
                }
                if ((amountChanged || accountChanged) && expense.getPaymentAccount() != null) {
                    createExpenseTransaction(expense, purchase.getReferenceNo());
                }
            } else {
                if (exDto.getPaymentAccountId() == null) {
                    throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_REQUIRED);
                }
                PaymentAccount paymentAccount = validatedAccounts.get(exDto.getPaymentAccountId());
                purchase.getPurchaseExpenses().add(getPurchaseExpense(exDto, purchase, paymentAccount, user));
            }
            totalExpenseAmt = totalExpenseAmt.add(exDto.getAmount());
        }
        return totalExpenseAmt;
    }

    @Transactional
    public PurchaseRs delete(Long id, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        Optional<Inventory> inventoryOpt = inventoryRepository.findByPurchaseOrderDetailPurchaseId(id);
        if (inventoryOpt.isPresent() && StatusEnum.SOLD.equals(inventoryOpt.get().getStatus())) {
            throw new IllegalStateException("Vehicle already sold.");
        }
        purchase.getPurchaseExpenses().forEach(this::reverseExpenseTransaction);
        purchase.getPayments().forEach(this::reversePaymentTransaction);
        if (!isExchangePurchase(id)) {
            journalService.reverse(JournalService.REF_PURCHASE, id);
        }
        purchaseRepository.delete(purchase);
        inventoryOpt.ifPresent(inventoryRepository::delete);
        return PurchaseRs.builder().build();
    }

    public PurchaseDTO get(Long id, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND));
        Inventory inventory = inventoryRepository.findByPurchaseOrderDetailPurchaseId(id).orElse(null);
        BigDecimal paidAmount = purchasePaymentRepository.sumAmountByPurchaseId(id);
        PurchaseDTO purchaseDTO = convertToDTO(purchase, inventory, paidAmount);
        List<ExpenseDTO> expenseDTOs = purchase.getPurchaseExpenses().stream().map(this::convertToExpenseDTO).toList();
        purchaseDTO.setExpenses(expenseDTOs);
        purchaseDTO.setTotalExpenses(expenseDTOs.stream().map(ExpenseDTO::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        purchaseDTO.setPayments(purchasePaymentRepository.findByPurchaseIdOrderByPaymentDateDesc(id)
                .stream().map(this::toPaymentDTO).toList());
        purchaseDTO.setEditable(computeIsEditableForDetail(purchase, inventory));
        return purchaseDTO;
    }

    @Transactional
    public PurchaseRs recordPayment(Long purchaseId, PurchasePaymentRq rq, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND));
        BigDecimal alreadyPaid = purchasePaymentRepository.sumAmountByPurchaseId(purchaseId);
        BigDecimal remaining = computeRemainingPayable(purchase, alreadyPaid);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(new ErrorCode.CustomError("PAY_400", "Purchase is already fully paid"));
        }
        if (rq.getAmount().compareTo(remaining) > 0) {
            throw new BusinessException(ErrorCode.Business.OVERPAYMENT);
        }
        PaymentAccount paymentAccount = resolvePaymentAccount(rq);
        PurchasePayment payment = new PurchasePayment();
        payment.setPurchase(purchase);
        payment.setAmount(rq.getAmount());
        payment.setPaymentDate(rq.getPaymentDate() != null ? rq.getPaymentDate() : LocalDate.now());
        payment.setPaymentMethod(rq.getPaymentMethod());
        payment.setReferenceNo(rq.getReferenceNo());
        payment.setNotes(rq.getNotes());
        payment.setPaymentAccount(paymentAccount);
        PurchasePayment saved = purchasePaymentRepository.save(payment);
        createTransaction(saved, purchase.getReferenceNo(), paymentAccount);
        return new PurchaseRs();
    }

    @Transactional
    public PurchaseRs updatePayment(Long purchaseId, Long paymentId, PurchasePaymentRq rq, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND));
        PurchasePayment payment = purchasePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND));
        if (!payment.getPurchase().getId().equals(purchaseId)) {
            throw new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND);
        }

        boolean amountChanged = payment.getAmount().compareTo(rq.getAmount()) != 0;
        Long oldAccountId = payment.getPaymentAccount() != null ? payment.getPaymentAccount().getId() : null;
        boolean accountChanged = !Objects.equals(oldAccountId, rq.getPaymentAccountId());

        if (amountChanged || accountChanged) {
            BigDecimal alreadyPaid = purchasePaymentRepository.sumAmountByPurchaseId(purchaseId);
            BigDecimal paidExcludingThis = alreadyPaid.subtract(payment.getAmount());
            BigDecimal remaining = computeRemainingPayable(purchase, paidExcludingThis);
            if (rq.getAmount().compareTo(remaining) > 0) {
                throw new BusinessException(ErrorCode.Business.OVERPAYMENT);
            }
            reversePaymentTransaction(payment);
        }

        payment.setAmount(rq.getAmount());
        payment.setPaymentDate(rq.getPaymentDate() != null ? rq.getPaymentDate() : payment.getPaymentDate());
        payment.setPaymentMethod(rq.getPaymentMethod());
        payment.setReferenceNo(rq.getReferenceNo());
        payment.setNotes(rq.getNotes());

        PaymentAccount newAccount = resolvePaymentAccount(rq);
        payment.setPaymentAccount(newAccount);
        PurchasePayment saved = purchasePaymentRepository.save(payment);

        if (amountChanged || accountChanged) {
            createTransaction(saved, purchase.getReferenceNo(), newAccount);
        }
        return new PurchaseRs();
    }

    @Transactional
    public PurchaseRs deletePayment(Long purchaseId, Long paymentId, UserDTO user) {
        PurchasePayment payment = purchasePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND));
        if (!payment.getPurchase().getId().equals(purchaseId)) {
            throw new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND);
        }
        reversePaymentTransaction(payment);
        purchasePaymentRepository.delete(payment);
        return new PurchaseRs();
    }

    private PaymentAccount resolvePaymentAccount(PurchasePaymentRq rq) {
        if (rq.getPaymentAccountId() == null) {
            if (PaymentMethodEnum.BANK.equals(rq.getPaymentMethod()) || PaymentMethodEnum.CHEQUE.equals(rq.getPaymentMethod()) || PaymentMethodEnum.CASH.equals(rq.getPaymentMethod())) {
                throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_REQUIRED);
            }
            return null;
        }
        return resolveAndValidateAccount(rq.getPaymentAccountId(), rq.getAmount());
    }

    private PaymentAccount resolveAndValidateAccount(Long accountId, BigDecimal amount) {
        PaymentAccount account = paymentAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));
        BigDecimal totalIn  = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.IN);
        BigDecimal totalOut = transactionRepository.sumAmountByAccountAndDirection(account.getId(), TransactionDirectionEnum.OUT);
        BigDecimal balance  = account.getOpeningBalance().add(totalIn).subtract(totalOut);
        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.Business.INSUFFICIENT_BALANCE);
        }
        return account;
    }

    private void createTransaction(PurchasePayment payment, String purchaseRefNo, PaymentAccount paymentAccount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionDate(payment.getPaymentDate());
        transaction.setType(TransactionTypeEnum.PURCHASE_PAYMENT);
        transaction.setReferenceType("PURCHASE_PAYMENT");
        transaction.setReferenceId(payment.getId());
        transaction.setPaymentAccount(paymentAccount);
        transaction.setAmount(payment.getAmount());
        transaction.setDirection(TransactionDirectionEnum.OUT);
        transaction.setDescription("Purchase payment – " + purchaseRefNo);
        transaction.setNotes(payment.getNotes());
        transactionRepository.save(transaction);
        journalService.post(JournalService.REF_PURCHASE_PAYMENT, payment.getId());

        // TODO [JOURNAL ENTRY] - Purchase Payment Made
        // Trigger  : every time a vendor payment is recorded (this method is the single entry point).
        // Entry    : settles the outstanding Accounts Payable against the cash/bank account.
        //   Dr  Accounts Payable–Vendor  (Liability)            payment.getAmount()  (reduces what we owe)
        //   Cr  <paymentAccount.name>    (Asset – Bank/Cash)    payment.getAmount()  (money leaves account)
        // Note: paymentAccount may be null for CASH payments where no account was selected;
        //       in that case Cr goes to a generic "Cash in Hand" CoA account.
        //       This entry is the counterpart to the Cr Accounts Payable from the purchase creation entry.
        // Future call: JournalEntryService.postPurchasePayment(transaction.getId())
        // CoA required: "Accounts Payable" (Liability), PaymentAccount's CoA entry (Asset).
    }

    private void createExpenseTransaction(Expense expense, String purchaseRefNo) {
        Transaction transaction = new Transaction();
        transaction.setTransactionDate(expense.getDate());
        transaction.setType(TransactionTypeEnum.PURCHASE_EXPENSE_PAYMENT);
        transaction.setReferenceType("PURCHASE_EXPENSE");
        transaction.setReferenceId(expense.getId());
        transaction.setPaymentAccount(expense.getPaymentAccount());
        transaction.setAmount(expense.getAmount());
        transaction.setDirection(TransactionDirectionEnum.OUT);
        transaction.setDescription("Purchase expense – " + purchaseRefNo);
        transactionRepository.save(transaction);
        journalService.post(JournalService.REF_EXPENSE, expense.getId());

        // TODO [JOURNAL ENTRY] - Purchase Expense Paid
        // Trigger  : when a purchase-linked expense is created with a paymentAccountId.
        // Entry    : records outflow for the expense from the selected payment account.
        //   Dr  <expense.expenseAccount.name>   (Expense)            expense.getAmount()
        //   Cr  <expense.paymentAccount.name>   (Asset – Bank/Cash)  expense.getAmount()
        // Note: Each expense line may debit a different payment account (per-expense paymentAccountId).
        // Future call: JournalEntryService.postPurchaseExpense(expense.getId())
        // CoA required: Expense ChartOfAccount (Expense.expenseAccount), PaymentAccount's CoA (Asset).
    }

    private void reverseExpenseTransaction(Expense expense) {
        transactionRepository.findActiveByReferenceTypeAndReferenceId("PURCHASE_EXPENSE", expense.getId())
                .ifPresent(original -> {
                    if (transactionRepository.existsByReversalOfId(original.getId())) return;
                    Transaction reversal = new Transaction();
                    reversal.setTransactionDate(LocalDate.now());
                    reversal.setType(TransactionTypeEnum.PURCHASE_EXPENSE_PAYMENT);
                    reversal.setReferenceType("PURCHASE_EXPENSE");
                    reversal.setReferenceId(expense.getId());
                    reversal.setPaymentAccount(original.getPaymentAccount());
                    reversal.setAmount(original.getAmount());
                    reversal.setDirection(TransactionDirectionEnum.IN);
                    reversal.setDescription("Reversal – " + original.getDescription());
                    reversal.setReversalOf(original);
                    transactionRepository.save(reversal);
                });
        journalService.reverse(JournalService.REF_EXPENSE, expense.getId());
    }

    private void reversePaymentTransaction(PurchasePayment payment) {
        transactionRepository.findActiveByReferenceTypeAndReferenceId("PURCHASE_PAYMENT", payment.getId())
                .ifPresent(original -> {
                    if (transactionRepository.existsByReversalOfId(original.getId())) return;
                    Transaction reversal = new Transaction();
                    reversal.setTransactionDate(LocalDate.now());
                    reversal.setType(TransactionTypeEnum.PURCHASE_PAYMENT);
                    reversal.setReferenceType("PURCHASE_PAYMENT");
                    reversal.setReferenceId(payment.getId());
                    reversal.setPaymentAccount(original.getPaymentAccount());
                    reversal.setAmount(original.getAmount());
                    reversal.setDirection(TransactionDirectionEnum.IN);
                    reversal.setDescription("Reversal – " + original.getDescription());
                    reversal.setReversalOf(original);
                    transactionRepository.save(reversal);
                });
        journalService.reverseOnDate(JournalService.REF_PURCHASE_PAYMENT, payment.getId(), LocalDate.now());
    }

    public PayablesSummaryRs getPayablesSummary() {
        List<PayableRow> rows = purchaseRepository.findPayables();
        List<PayableInfo> items = rows.stream().map(r -> PayableInfo.builder()
                .purchaseId(r.getPurchaseId())
                .referenceNo(r.getReferenceNo())
                .vehicleNo(r.getVehicleNo())
                .purchaseDate(r.getPurchaseDate())
                .amount(safe(r.getAmount()))
                .pendingAmount(safe(r.getPendingAmount()))
                .lastPaymentDate(r.getLastPaymentDate())
                .vendorName(r.getVendorName())
                .vendorMobile(r.getVendorMobile())
                .build()).toList();
        BigDecimal totalPending = items.stream()
                .map(PayableInfo::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return PayablesSummaryRs.builder()
                .totalCount(items.size())
                .totalPendingAmount(totalPending)
                .items(items)
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean isExchangePurchase(Long purchaseId) {
        // True only for exchange purchases that have NOT yet been promoted to standalone
        // via KEEP_AND_BUYBACK. Once buyback_recorded_at is set, the purchase behaves as
        // a normal vendor purchase for update/delete/payables flows.
        return inventoryRepository.findByPurchaseOrderDetailPurchaseId(purchaseId)
                .map(inv -> inv.getSourceSaleId() != null
                        && inv.getPurchaseOrderDetail().getPurchase().getBuybackRecordedAt() == null)
                .orElse(false);
    }

    private PurchasePaymentDTO toPaymentDTO(PurchasePayment p) {
        return PurchasePaymentDTO.builder()
                .id(p.getId())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod())
                .paymentAccountId(p.getPaymentAccount() != null ? p.getPaymentAccount().getId() : null)
                .paymentAccountName(p.getPaymentAccount() != null ? p.getPaymentAccount().getName() : null)
                .referenceNo(p.getReferenceNo())
                .notes(p.getNotes())
                .build();
    }

    private ExpenseDTO convertToExpenseDTO(Expense expense) {
        return ExpenseDTO.builder()
                .id(expense.getId())
                .date(expense.getDate())
                .typeId(expense.getExpenseAccount().getId())
                .title(expense.getExpenseAccount().getName())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .paymentAccountId(expense.getPaymentAccount() != null ? expense.getPaymentAccount().getId() : null)
                .build();
    }

    private Product findOrCreateProduct(PurchaseRq purchaseRq) {
        return productRepository.findByBrandIdAndModelIdAndVarientId(purchaseRq.getBrandId(), purchaseRq.getModelId(), purchaseRq.getVariantId()).orElseGet(() -> productService.createProduct(this.convertToProductRq(purchaseRq)));
    }

    private ProductRq convertToProductRq(PurchaseRq purchaseRq) {
        return ProductRq.builder().brandId(purchaseRq.getBrandId()).modelId(purchaseRq.getModelId()).varientId(purchaseRq.getVariantId()).segmentId(purchaseRq.getSegmentId()).build();
    }

    private Long parseOdometer(String odometer) {
        if (!StringUtils.hasLength(odometer)) return null;
        try {
            return Long.parseLong(odometer.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private void createInventoryRecord(Purchase p, PurchaseDetail d, Product prod, PurchaseRq purchaseRq, BigDecimal totalExpenses, StatusEnum status) {
        Inventory inventory = new Inventory();
        inventory.setProduct(prod);
        inventory.setPurchaseOrderDetail(d);
        inventory.setUin(StringUtils.hasLength(purchaseRq.getCode()) ? purchaseRq.getCode() : purchaseRq.getVehicleNo());
        inventory.setProductNo(purchaseRq.getVehicleNo());
        inventory.setOdometer(parseOdometer(purchaseRq.getOdometer()));
        inventory.setColor(Objects.nonNull(purchaseRq.getColorId()) ? lookupHelper.get(purchaseRq.getColorId()) : null);
        inventory.setMakeYear(purchaseRq.getMakeYear());
        inventory.setWarehouseId(purchaseRq.getWarehouseId());
        inventory.setStatus(status);
        // receivedDate is only set when the vehicle is physically received
        if (StatusEnum.AVAILABLE.equals(status)) {
            inventory.setReceivedDate(purchaseRq.getDeliveredDate() != null
                    ? purchaseRq.getDeliveredDate().atStartOfDay()
                    : LocalDateTime.now());
        }
        BigDecimal landedCost = purchaseRq.getPurchaseRate().add(totalExpenses);
        inventory.setLandedCost(landedCost);
        if (Objects.nonNull(purchaseRq.getSourceSaleId()))
            inventory.setSourceSaleId(purchaseRq.getSourceSaleId());
        inventoryRepository.save(inventory);
    }
}
