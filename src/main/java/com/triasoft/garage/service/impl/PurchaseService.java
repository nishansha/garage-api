package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.LookupTypeEnum;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.*;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.product.ProductRq;
import com.triasoft.garage.model.purchase.PurchaseRq;
import com.triasoft.garage.model.purchase.PurchaseRs;
import com.triasoft.garage.model.purchase.PurchaseSummaryRs;
import com.triasoft.garage.projection.PurchaseMetrics;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final ProductService productService;
    private final AccountService accountService;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final VendorRepository vendorRepository;
    private final InventoryRepository inventoryRepository;
    private final LookupHelper lookupHelper;


    public PurchaseRs getAll(Pageable pageable, UserDTO user) {
        Page<Purchase> purchasePage = purchaseRepository.findAll(pageable);
        List<PurchaseDTO> purchases = purchasePage.getContent().stream().map(this::convertToDTO).toList();
        PurchaseRs purchaseRs = PurchaseRs.builder().purchases(purchases).build();
        purchaseRs.setTotalPages(purchasePage.getTotalPages());
        purchaseRs.setTotalElements(purchasePage.getTotalElements());
        return purchaseRs;
    }

    private PurchaseDTO convertToDTO(Purchase purchase) {
        PurchaseDetail purchaseDetail = purchase.getPurchaseDetails().get(0);
        return PurchaseDTO.builder()
                .id(purchase.getId())
                .date(purchase.getOrderDate())
                .code(purchaseDetail.getUuid())
                .vehicleNo(purchaseDetail.getProductNo())
                .brandName(purchaseDetail.getProduct().getBrand().getDescription())
                .brandId(purchaseDetail.getProduct().getBrand().getId())
                .modelName(purchaseDetail.getProduct().getModel().getDescription())
                .modelId(purchaseDetail.getProduct().getModel().getId())
                .variantName(purchaseDetail.getProduct().getVarient().getDescription())
                .variantId(purchaseDetail.getProduct().getVarient().getId())
                .purchaseRate(purchaseDetail.getUnitCost())
                .makeYear(purchaseDetail.getProduct().getMakeYear())
                .odometer(purchaseDetail.getOdometer())
                .pickupLocation(purchase.getPickupLocation())
                .pickupStaffId(purchase.getPickupStaffId())
                .notes(purchase.getNotes())
                .ownerName(purchase.getVendor().getName())
                .ownerMobileNo(purchase.getVendor().getMobile())
                .ownerShipSerialNo(purchaseDetail.getOwnershipSerialNo())
                .isSold(false)
                .build();
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
        List<PurchaseDTO> purchases = purchasePage.getContent().stream().map(this::convertToDTO).toList();
        PurchaseRs purchaseRs = PurchaseRs.builder().purchases(purchases).build();
        purchaseRs.setTotalPages(purchasePage.getTotalPages());
        purchaseRs.setTotalElements(purchasePage.getTotalElements());
        return purchaseRs;
    }

    @Transactional
    public PurchaseRs create(PurchaseRq purchaseRq, UserDTO user) {
        Vendor vendor = vendorRepository.findByMobile(purchaseRq.getOwnerMobileNo()).orElseGet(() -> createVendor(purchaseRq, user));
        Product product = findOrCreateProduct(purchaseRq);
        Purchase purchase = new Purchase();
        purchase.setVendor(vendor);
        purchase.setReferenceNo("PO-" + purchaseRepository.getNextReferenceNumber());
        purchase.setOrderDate(purchaseRq.getDate());
        purchase.setStatus(lookupHelper.getStatus(LookupTypeEnum.PURCHASE_STATUS, StatusEnum.RECIEVED));
        purchase.setNotes(purchaseRq.getNotes());
        BigDecimal totalExpenseAmt = createAndGetExpense(purchaseRq, purchase, user);
        PurchaseDetail detail = getPurchaseDetail(purchaseRq, product, purchase);
        purchase.setPurchaseDetails(List.of(detail));
        purchase.setTotalAmount(purchaseRq.getPurchaseRate().add(totalExpenseAmt));
        purchase.setPickupLocation(purchaseRq.getPickupLocation());
        purchase.setPickupStaffId(purchaseRq.getPickupStaffId());
        Purchase savedPurchase = purchaseRepository.save(purchase);
        createInventoryRecord(savedPurchase, detail, product, purchaseRq, totalExpenseAmt);
        return new PurchaseRs();
    }

    @Transactional
    public PurchaseRs update(Long purchaseId, PurchaseRq purchaseRq, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(purchaseId).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        Inventory inventory = inventoryRepository.findByPurchaseOrderDetailPurchaseId(purchaseId).orElseThrow(() -> new EntityNotFoundException("Inventory record missing"));
        if (StatusEnum.SOLD.equals(inventory.getStatus())) {
            throw new IllegalStateException("Cannot update a purchase for a vehicle already sold.");
        }
        updateVendor(purchase, purchaseRq, user);
        Product product = findOrCreateProduct(purchaseRq);
        purchase.setOrderDate(purchaseRq.getDate());
        purchase.setPickupLocation(purchaseRq.getPickupLocation());
        purchase.setPickupStaffId(purchaseRq.getPickupStaffId());
        purchase.setNotes(purchaseRq.getNotes());
        BigDecimal totalExpenseAmt = createAndGetExpense(purchaseRq, purchase, user);
        purchase.setTotalAmount(purchaseRq.getPurchaseRate().add(totalExpenseAmt));
        PurchaseDetail detail = purchase.getPurchaseDetails().get(0);
        detail.setProduct(product);
        detail.setUnitCost(purchaseRq.getPurchaseRate());
        detail.setOwnershipSerialNo(purchaseRq.getOwnerShipSerialNo());
        purchaseRepository.save(purchase);
        inventory.setProduct(product);
        inventory.setProductNo(purchaseRq.getVehicleNo());
        inventory.setOdometer(Long.parseLong(purchaseRq.getOdometer()));
        inventory.setUin(purchaseRq.getOwnerShipSerialNo());
        inventory.setLandedCost(purchaseRq.getPurchaseRate().add(totalExpenseAmt));
        inventoryRepository.save(inventory);
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
        detail.setUuid(purchaseRq.getCode());
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
        BigDecimal totalExpenseAmt = BigDecimal.ZERO;
        for (ExpenseDTO exDto : purchaseRq.getExpenses()) {
            Expense expense;
            if (Objects.nonNull(exDto.getId())) {
                expense = purchase.getPurchaseExpenses().stream().filter(e -> e.getId().equals(exDto.getId())).findFirst().orElseThrow(() -> new BusinessException(ErrorCode.Business.EXP_NOT_FOUNT));
                expense.setDate(exDto.getDate());
                expense.setAmount(exDto.getAmount());
                expense.setDescription(exDto.getDescription());
                expense.setExpenseAccount(accountService.getOrCreateExpenseAccount(exDto, user));
            } else {
                expense = getPurchaseExpense(exDto, purchase, user);
            }
            purchase.getPurchaseExpenses().add(expense);
            totalExpenseAmt = totalExpenseAmt.add(exDto.getAmount());
        }
        return totalExpenseAmt;
    }

    public PurchaseRs delete(Long id, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        Inventory inventory = inventoryRepository.findByPurchaseOrderDetailPurchaseId(id).orElseThrow(() -> new EntityNotFoundException("Inventory record missing"));
        if (StatusEnum.SOLD.equals(inventory.getStatus())) {
            throw new IllegalStateException("Vehicle already sold.");
        }
        purchaseRepository.delete(purchase);
        inventoryRepository.delete(inventory);
        return PurchaseRs.builder().build();
    }

    public PurchaseDTO get(Long id, UserDTO user) {
        Purchase purchase = purchaseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.PURCHASE_NOT_FOUND));
        PurchaseDTO purchaseDTO = convertToDTO(purchase);
        List<ExpenseDTO> expenses = purchase.getPurchaseExpenses().stream().map(this::convertToExpenseDTO).toList();
        purchaseDTO.setExpenses(expenses);
        return purchaseDTO;
    }

    private ExpenseDTO convertToExpenseDTO(Expense expense) {
        return ExpenseDTO.builder().id(expense.getId()).date(expense.getCreatedAt().toLocalDate()).typeId(expense.getExpenseAccount().getId()).description(expense.getDescription()).amount(expense.getAmount()).build();
    }

    private Product findOrCreateProduct(PurchaseRq purchaseRq) {
        return productRepository.findByBrandIdAndModelIdAndVarientIdAndMakeYear(purchaseRq.getBrandId(), purchaseRq.getModelId(), purchaseRq.getVariantId(), purchaseRq.getMakeYear()).orElseGet(() -> productService.createProduct(this.convertToProductRq(purchaseRq)));
    }

    private ProductRq convertToProductRq(PurchaseRq purchaseRq) {
        return ProductRq.builder().brandId(purchaseRq.getBrandId()).modelId(purchaseRq.getModelId()).varientId(purchaseRq.getVariantId()).makeYear(purchaseRq.getMakeYear()).build();
    }


    private void createInventoryRecord(Purchase p, PurchaseDetail d, Product prod, PurchaseRq purchaseRq, BigDecimal totalExpenses) {
        Inventory inventory = new Inventory();
        inventory.setProduct(prod);
        inventory.setPurchaseOrderDetail(d);
        inventory.setUin(purchaseRq.getCode()); // this should be the chassis number
        inventory.setProductNo(purchaseRq.getVehicleNo());
        inventory.setOdometer(Long.parseLong(purchaseRq.getOdometer()));
        inventory.setColor(purchaseRq.getColor());
        inventory.setWarehouseId(purchaseRq.getWarehouseId());
        inventory.setStatus(StatusEnum.AVAILABLE);
        inventory.setReceivedDate(LocalDateTime.now());
        BigDecimal landedCost = purchaseRq.getPurchaseRate().add(totalExpenses);
        inventory.setLandedCost(landedCost);
        if (Objects.nonNull(purchaseRq.getSourceSaleId()))
            inventory.setSourceSaleId(purchaseRq.getSourceSaleId());
        inventoryRepository.save(inventory);
    }
}
