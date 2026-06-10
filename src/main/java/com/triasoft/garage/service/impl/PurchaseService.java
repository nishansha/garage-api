package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.LookupTypeEnum;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.dto.PurchasePaymentDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.*;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.product.ProductRq;
import com.triasoft.garage.model.purchase.PurchasePaymentRq;
import com.triasoft.garage.model.purchase.PurchaseRq;
import com.triasoft.garage.model.purchase.PurchaseRs;
import com.triasoft.garage.model.purchase.PurchaseSummaryRs;
import com.triasoft.garage.projection.PurchasePaidProjection;
import com.triasoft.garage.projection.PurchaseMetrics;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.ProductRepository;
import com.triasoft.garage.repository.PurchasePaymentRepository;
import com.triasoft.garage.repository.PurchaseRepository;
import com.triasoft.garage.repository.VendorRepository;
import com.triasoft.garage.specifiction.PurchaseSpecification;
import com.triasoft.garage.util.CommonUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final ProductService productService;
    private final AccountService accountService;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final VendorRepository vendorRepository;
    private final InventoryRepository inventoryRepository;
    private final LookupHelper lookupHelper;


    public PurchaseRs getAll(Pageable pageable, UserDTO user) {
        Page<Purchase> purchasePage = purchaseRepository.findAll(pageable);
        List<Purchase> content = purchasePage.getContent();
        List<Long> ids = content.stream().map(Purchase::getId).toList();
        Map<Long, Inventory> inventoryMap = fetchInventoryMap(ids);
        Map<Long, BigDecimal> paidMap = getPaidAmountMap(ids);
        List<PurchaseDTO> purchases = content.stream()
                .map(p -> convertToDTO(p, inventoryMap.get(p.getId()), paidMap.getOrDefault(p.getId(), BigDecimal.ZERO)))
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
        BigDecimal pending = unitCost.subtract(paidAmount);
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
                .purchaseRate(purchaseDetail.getUnitCost())
                .totalCost(purchase.getTotalAmount())
                .paidAmount(paidAmount)
                .pendingAmount(pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending)
                .paymentStatus(derivePaymentStatus(paidAmount, unitCost))
                .makeYear(inventory != null ? inventory.getMakeYear() : null)
                .color(inventory != null ? inventory.getColor() : null)
                .warehouseId(inventory != null ? inventory.getWarehouseId() : null)
                .odometer(purchaseDetail.getOdometer())
                .pickupLocation(purchase.getPickupLocation())
                .pickupStaffId(purchase.getPickupStaffId())
                .notes(purchase.getNotes())
                .ownerName(purchase.getVendor().getName())
                .ownerMobileNo(purchase.getVendor().getMobile())
                .ownerShipSerialNo(purchaseDetail.getOwnershipSerialNo())
                .isSold(inventory != null && StatusEnum.SOLD.equals(inventory.getStatus()))
                .build();
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

    private Map<Long, Inventory> fetchInventoryMap(List<Long> purchaseIds) {
        if (CollectionUtils.isEmpty(purchaseIds)) return Map.of();
        return inventoryRepository.findByPurchaseOrderDetailPurchaseIdIn(purchaseIds).stream()
                .collect(Collectors.toMap(
                        i -> i.getPurchaseOrderDetail().getPurchase().getId(),
                        i -> i,
                        (a, b) -> a
                ));
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
        Specification<Purchase> spec = PurchaseSpecification.buildSearchQuery(filterRq);
        Page<Purchase> purchasePage = purchaseRepository.findAll(spec, pageable);
        List<Purchase> content = purchasePage.getContent();
        List<Long> ids = content.stream().map(Purchase::getId).toList();
        Map<Long, Inventory> inventoryMap = fetchInventoryMap(ids);
        Map<Long, BigDecimal> paidMap = getPaidAmountMap(ids);
        List<PurchaseDTO> purchases = content.stream()
                .map(p -> convertToDTO(p, inventoryMap.get(p.getId()), paidMap.getOrDefault(p.getId(), BigDecimal.ZERO)))
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
        StatusEnum inventoryStatus = purchaseRq.getDeliveredDate() != null ? StatusEnum.AVAILABLE : StatusEnum.PENDING_DELIVERY;
        createInventoryRecord(savedPurchase, detail, product, purchaseRq, totalExpenseAmt, inventoryStatus);
        return rs;
    }

    @Transactional
    public PurchaseRs update(Long purchaseId, PurchaseRq purchaseRq, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(purchaseId).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        Optional<Inventory> inventoryOpt = inventoryRepository.findByPurchaseOrderDetailPurchaseId(purchaseId);
        if (inventoryOpt.isPresent() && StatusEnum.SOLD.equals(inventoryOpt.get().getStatus())) {
            throw new IllegalStateException("Cannot update a purchase for a vehicle already sold.");
        }
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
            purchase.getPurchaseExpenses().removeIf(e -> !incomingIds.contains(e.getId()));
        } else {
            purchase.getPurchaseExpenses().clear();
        }

        BigDecimal totalExpenseAmt = createAndGetExpense(purchaseRq, purchase, user);
        purchase.setTotalAmount(purchaseRq.getPurchaseRate().add(totalExpenseAmt));
        if (CollectionUtils.isEmpty(purchase.getPurchaseDetails())) {
            throw new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND);
        }
        PurchaseDetail detail = purchase.getPurchaseDetails().get(0);
        detail.setProduct(product);
        detail.setUnitCost(purchaseRq.getPurchaseRate());
        detail.setOwnershipSerialNo(purchaseRq.getOwnerShipSerialNo());
        purchaseRepository.save(purchase);

        if (inventoryOpt.isPresent()) {
            Inventory inventory = inventoryOpt.get();
            inventory.setProduct(product);
            inventory.setProductNo(purchaseRq.getVehicleNo());
            inventory.setOdometer(parseOdometer(purchaseRq.getOdometer()));
            inventory.setUin(StringUtils.hasLength(purchaseRq.getCode()) ? purchaseRq.getCode() : purchaseRq.getVehicleNo());
            inventory.setMakeYear(purchaseRq.getMakeYear());
            inventory.setColor(purchaseRq.getColor());
            inventory.setWarehouseId(purchaseRq.getWarehouseId());
            inventory.setLandedCost(purchaseRq.getPurchaseRate().add(totalExpenseAmt));
            // PENDING_DELIVERY → AVAILABLE: vehicle has now been received
            if (StatusEnum.PENDING_DELIVERY.equals(inventory.getStatus()) && purchaseRq.getDeliveredDate() != null) {
                inventory.setStatus(StatusEnum.AVAILABLE);
                inventory.setReceivedDate(purchaseRq.getDeliveredDate().atStartOfDay());
            }
            inventoryRepository.save(inventory);
        }

        return new PurchaseRs();
    }

    private Expense getPurchaseExpense(ExpenseDTO exDto, Purchase purchase, UserDTO user) {
        Expense expense = new Expense();
        expense.setDate(purchase.getOrderDate());
        expense.setAmount(exDto.getAmount());
        expense.setDescription(exDto.getDescription());
        expense.setPurchase(purchase);
        expense.setExpenseAccount(accountService.getOrCreateExpenseAccount(exDto, user));
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
        BigDecimal totalExpenseAmt = BigDecimal.ZERO;
        for (ExpenseDTO exDto : purchaseRq.getExpenses()) {
            if (Objects.nonNull(exDto.getId())) {
                Expense expense = purchase.getPurchaseExpenses().stream()
                        .filter(e -> e.getId().equals(exDto.getId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));
                expense.setDate(exDto.getDate());
                expense.setAmount(exDto.getAmount());
                expense.setDescription(exDto.getDescription());
                expense.setExpenseAccount(accountService.getOrCreateExpenseAccount(exDto, user));
            } else {
                purchase.getPurchaseExpenses().add(getPurchaseExpense(exDto, purchase, user));
            }
            totalExpenseAmt = totalExpenseAmt.add(exDto.getAmount());
        }
        return totalExpenseAmt;
    }

    public PurchaseRs delete(Long id, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        Optional<Inventory> inventoryOpt = inventoryRepository.findByPurchaseOrderDetailPurchaseId(id);
        if (inventoryOpt.isPresent() && StatusEnum.SOLD.equals(inventoryOpt.get().getStatus())) {
            throw new IllegalStateException("Vehicle already sold.");
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
        purchaseDTO.setExpenses(purchase.getPurchaseExpenses().stream().map(this::convertToExpenseDTO).toList());
        purchaseDTO.setPayments(purchasePaymentRepository.findByPurchaseIdOrderByPaymentDateDesc(id)
                .stream().map(this::toPaymentDTO).toList());
        return purchaseDTO;
    }

    @Transactional
    public PurchaseRs recordPayment(Long purchaseId, PurchasePaymentRq rq, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND));
        BigDecimal alreadyPaid = purchasePaymentRepository.sumAmountByPurchaseId(purchaseId);
        BigDecimal remaining = purchase.getTotalAmount().subtract(alreadyPaid);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(new ErrorCode.CustomError("PAY_400", "Purchase is already fully paid"));
        }
        PurchasePayment payment = new PurchasePayment();
        payment.setPurchase(purchase);
        payment.setAmount(rq.getAmount());
        payment.setPaymentDate(rq.getPaymentDate() != null ? rq.getPaymentDate() : LocalDate.now());
        payment.setPaymentMethod(rq.getPaymentMethod());
        payment.setReferenceNo(rq.getReferenceNo());
        payment.setNotes(rq.getNotes());
        purchasePaymentRepository.save(payment);
        return new PurchaseRs();
    }

    private PurchasePaymentDTO toPaymentDTO(PurchasePayment p) {
        return PurchasePaymentDTO.builder()
                .id(p.getId())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod())
                .referenceNo(p.getReferenceNo())
                .notes(p.getNotes())
                .build();
    }

    private ExpenseDTO convertToExpenseDTO(Expense expense) {
        return ExpenseDTO.builder().id(expense.getId()).date(expense.getCreatedAt().toLocalDate()).typeId(expense.getExpenseAccount().getId()).description(expense.getDescription()).amount(expense.getAmount()).build();
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
        inventory.setColor(purchaseRq.getColor());
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
