package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.dto.LookupDTO;
import com.triasoft.garage.dto.StockDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.common.LookupRs;
import com.triasoft.garage.model.stock.StockRs;
import com.triasoft.garage.model.stock.StockSummaryRs;
import com.triasoft.garage.projection.StockMetrics;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.specifiction.StockSpecification;
import com.triasoft.garage.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StockService {

    private final InventoryRepository inventoryRepository;

    public StockRs getAll(Pageable pageable, UserDTO user) {
        Page<Inventory> page = inventoryRepository.findAll(pageable);
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
        return StockDTO.builder()
                .productId(inventory.getId())
                .purchaseDate(inventory.getReceivedDate().toLocalDate())
                .productCode(inventory.getProductNo())
                .brandName(inventory.getProduct().getBrand().getDescription())
                .modelName(inventory.getProduct().getModel().getDescription())
                .variantName(inventory.getProduct().getVarient().getDescription())
                .purchasedAmount(inventory.getPurchaseOrderDetail().getUnitCost())
                .purchaseExpense(inventory.getLandedCost().subtract(inventory.getPurchaseOrderDetail().getUnitCost()))
                .status(inventory.getStatus().name())
                .color(inventory.getColor())
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
}
