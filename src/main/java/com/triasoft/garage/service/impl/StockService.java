package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.dto.ExpenseDTO;
import com.triasoft.garage.dto.LookupDTO;
import com.triasoft.garage.dto.StockDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.Expense;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.common.LookupRs;
import com.triasoft.garage.model.stock.StockRs;
import com.triasoft.garage.model.stock.StockSummaryRs;
import com.triasoft.garage.projection.StockMetrics;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.SaleRepository;
import com.triasoft.garage.specifiction.StockSpecification;
import com.triasoft.garage.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StockService {

    private final InventoryRepository inventoryRepository;
    private final SaleRepository saleRepository;

    public StockRs getAll(Pageable pageable,String status, UserDTO user) {

        List<StatusEnum> statuses = List.of(StatusEnum.AVAILABLE, StatusEnum.PENDING_DELIVERY);
        if(StringUtils.hasLength(status)){
            statuses = List.of(StatusEnum.valueOf(status));
        }
        Page<Inventory> page = inventoryRepository.findByStatusIn(statuses, pageable);
        List<StockDTO> products = page.getContent().stream().map(this::convertToDTO).toList();
        StockRs stockRs = StockRs.builder().products(products).build();
        stockRs.setTotalPages(page.getTotalPages());
        stockRs.setTotalElements(page.getTotalElements());
        return stockRs;
    }

    public StockSummaryRs summary(UserDTO user) {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        StockMetrics metrics = inventoryRepository.getStockSummaryMetrics(startOfMonth);
        double assetRate = CommonUtil.calculateDelta(
                metrics.getTotalStockValue(),
                Objects.nonNull(metrics.getTotalStockValueLastMonth()) ? metrics.getTotalStockValueLastMonth() : BigDecimal.ZERO
        );
        return StockSummaryRs.builder()
                .totalItems(metrics.getTotalItems())
                .stockValue(metrics.getTotalStockValue() != null ? metrics.getTotalStockValue().toString() : "0")
                .itemsThisMonth(metrics.getItemsAddedThisMonth())
                .assetRate(assetRate)
                .build();
    }

    public StockRs findProducts(FilterRq filterRq, Pageable pageable, UserDTO user) {
        Specification<Inventory> spec = StockSpecification.buildSearchQuery(filterRq);
        Page<Inventory> page = inventoryRepository.findAll(spec, pageable);
        List<StockDTO> products = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();
        StockRs stockRs = StockRs.builder().products(products).build();
        stockRs.setTotalPages(page.getTotalPages());
        stockRs.setTotalElements(page.getTotalElements());
        return stockRs;
    }

    private StockDTO convertToDTO(Inventory inventory) {
        var purchase = inventory.getPurchaseOrderDetail().getPurchase();
        var vendor = purchase.getVendor();
        return StockDTO.builder()
                .productId(inventory.getId())
                .purchaseDate(Objects.nonNull(inventory.getReceivedDate()) ? inventory.getReceivedDate().toLocalDate() : null)
                .productCode(inventory.getProductNo())
                .brandName(inventory.getProduct().getBrand().getDescription())
                .modelName(inventory.getProduct().getModel().getDescription())
                .variantName(inventory.getProduct().getVarient().getDescription())
                .fuelTypeId(inventory.getProduct().getFuelType() != null ? inventory.getProduct().getFuelType().getId() : null)
                .fuelType(inventory.getProduct().getFuelType() != null ? inventory.getProduct().getFuelType().getDescription() : null)
                .purchasedAmount(inventory.getPurchaseOrderDetail().getUnitCost())
                .purchaseExpense(inventory.getLandedCost().subtract(inventory.getPurchaseOrderDetail().getUnitCost()))
                .landedCost(inventory.getLandedCost())
                .totalAmount(purchase.getTotalAmount())
                .vendorName(vendor.getName())
                .vendorMobileNo(vendor.getMobile())
                .status(inventory.getStatus().name())
                .color(Objects.nonNull(inventory.getColor()) ? inventory.getColor().getDescription() : null)
                .odometer(inventory.getOdometer())
                .build();
    }

    public LookupRs stockProducts(UserDTO user) {
        List<Inventory> availableProducts = inventoryRepository.findAllByStatus(StatusEnum.AVAILABLE);
        List<LookupDTO> products = availableProducts.stream().map(p -> LookupDTO.builder()
                .id(p.getId())
                .code(p.getProductNo())
                .description(p.getProductNo())
                .build()).toList();
        return LookupRs.builder().values(products).build();
    }

    public StockDTO get(Long id, UserDTO user) {
        Inventory inventory = inventoryRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.Business.PRD_NOT_FOUND));
        StockDTO stockDTO = convertToDTO(inventory);
        if (Objects.nonNull(inventory.getPurchaseOrderDetail().getPurchase().getPurchaseExpenses())) {
            List<ExpenseDTO> expenses = inventory.getPurchaseOrderDetail().getPurchase().getPurchaseExpenses().stream()
                    .map(this::convertToExpenseDTO).toList();
            stockDTO.setExpenses(expenses);
        }
        if (StatusEnum.SOLD.equals(inventory.getStatus())) {
            Sale sale = saleRepository.findByInventoryId(id);
            if(Objects.nonNull(sale)){
                stockDTO.setSoldDate(sale.getSaleDate());
                stockDTO.setSoldAmount(sale.getNetSaleAmount());
            }
        }
        return stockDTO;
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
}
