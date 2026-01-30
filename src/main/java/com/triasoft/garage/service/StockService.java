package com.triasoft.garage.service;

import com.triasoft.garage.dto.SaleDTO;
import com.triasoft.garage.dto.StockDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.sale.SaleSummaryRs;
import com.triasoft.garage.model.sale.SalesRs;
import com.triasoft.garage.model.stock.StockRs;
import com.triasoft.garage.model.stock.StockSummaryRs;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    public StockRs products(Pageable pageable, UserDTO user) {
        List<StockDTO> products = new ArrayList<>();
        products.add(StockDTO.builder()
                .productId(1L)
                .purchaseDate(LocalDate.now())
                .productCode("KL 10 BE 1067")
                .brandName("Toyota")
                .modelName("Corolla")
                .variantName("Base")
                .purchaseExpense(new BigDecimal("5000"))
                .purchasedAmount(new BigDecimal("1450000"))
                .build());
        products.add(StockDTO.builder()
                .productId(2L)
                .purchaseDate(LocalDate.now())
                .productCode("KL 10 BE 1068")
                .brandName("Nissan")
                .modelName("Sunny")
                .variantName("XL")
                .purchaseExpense(new BigDecimal("1000"))
                .purchasedAmount(new BigDecimal("10000"))
                .build());
        StockRs stockRs = StockRs.builder().products(products).build();
        stockRs.setTotalPages(1);
        stockRs.setTotalElements(2L);
        return stockRs;
    }

    public StockSummaryRs summary(UserDTO user) {
        return StockSummaryRs.builder().stockValue("25,00,000").assetRate(25.5).totalItems(14L).itemsThisMonth(1L).build();
    }

    public StockRs findProducts(FilterRq filterRq, Pageable pageable, UserDTO user) {
        List<StockDTO> products = new ArrayList<>();
        products.add(StockDTO.builder()
                .productId(2L)
                .purchaseDate(LocalDate.now())
                .productCode("KL 10 BE 1068")
                .brandName("Nissan")
                .modelName("Sunny")
                .variantName("XL")
                .purchaseExpense(new BigDecimal("1000"))
                .purchasedAmount(new BigDecimal("10000"))
                .build());
        StockRs stockRs = StockRs.builder().products(products).build();
        stockRs.setTotalPages(1);
        stockRs.setTotalElements(2L);
        return stockRs;
    }
}
