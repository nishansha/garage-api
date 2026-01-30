package com.triasoft.garage.service;

import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.purchase.PurchaseRs;
import com.triasoft.garage.model.purchase.PurchaseSummaryRs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    public PurchaseRs purchases(Pageable pageable, UserDTO user) {
        List<PurchaseDTO> purchases = new ArrayList<>();
        purchases.add(PurchaseDTO.builder()
                .id(1L)
                .date(LocalDate.now())
                .vehicleNo("KL 10 BE 1067")
                .brandName("Toyota")
                .modelName("Corolla")
                .variantName("Base")
                .purchaseRate(new BigDecimal("1450000"))
                .isSold(false)
                .build());
        purchases.add(PurchaseDTO.builder()
                .id(2L)
                .date(LocalDate.now())
                .vehicleNo("KL 10 BE 1068")
                .brandName("Nissan")
                .modelName("Sunny")
                .variantName("XL")
                .purchaseRate(new BigDecimal("150000"))
                .isSold(true)
                .build());
        PurchaseRs purchaseRs = PurchaseRs.builder().purchases(purchases).build();
        purchaseRs.setTotalPages(1);
        purchaseRs.setTotalElements(2L);
        return purchaseRs;
    }

    public PurchaseSummaryRs summary(UserDTO user) {
        return PurchaseSummaryRs.builder().totalThisMonth("15,00,000").monthRate(10.6).todayCount(3L).todayCount(1L).build();
    }

    public PurchaseRs searchPurchase(FilterRq filterRq, Pageable pageable, UserDTO user) {
        List<PurchaseDTO> purchases = new ArrayList<>();
        purchases.add(PurchaseDTO.builder()
                .id(2L)
                .date(LocalDate.now())
                .vehicleNo("KL 10 BE 1068")
                .brandName("Nissan")
                .modelName("Sunny")
                .variantName("XL")
                .purchaseRate(new BigDecimal("150000"))
                .isSold(true)
                .build());
        PurchaseRs purchaseRs = PurchaseRs.builder().purchases(purchases).build();
        purchaseRs.setTotalPages(1);
        purchaseRs.setTotalElements(1L);
        return purchaseRs;
    }
}
