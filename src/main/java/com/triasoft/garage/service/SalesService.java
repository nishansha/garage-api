package com.triasoft.garage.service;

import com.triasoft.garage.dto.SaleDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.sale.SaleSummaryRs;
import com.triasoft.garage.model.sale.SalesRs;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SalesService {

    public SalesRs sales(Pageable pageable, UserDTO user) {
        List<SaleDTO> sales = new ArrayList<>();
        sales.add(SaleDTO.builder()
                .id(1L)
                .date(LocalDate.now())
                .vehicleNo("KL 10 BE 1067")
                .brandName("Toyota")
                .modelName("Corolla")
                .variantName("Base")
                .saleRate(new BigDecimal("1450000"))
                .profit(new BigDecimal("15000"))
                .build());
        sales.add(SaleDTO.builder()
                .id(2L)
                .date(LocalDate.now())
                .vehicleNo("KL 10 BE 1068")
                .brandName("Nissan")
                .modelName("Sunny")
                .variantName("XL")
                .saleRate(new BigDecimal("150000"))
                        .profit(new BigDecimal("10000"))
                .build());
        SalesRs salesRs = SalesRs.builder().sales(sales).build();
        salesRs.setTotalPages(1);
        salesRs.setTotalElements(2L);
        return salesRs;
    }

    public SaleSummaryRs summary(UserDTO user) {
        return SaleSummaryRs.builder().totalThisMonth("25,00,000").monthRate(25.5).totalCount(4L).todayCount(1L).build();
    }

    public SalesRs findSales(FilterRq filterRq, Pageable pageable, UserDTO user) {
        List<SaleDTO> sales = new ArrayList<>();
        sales.add(SaleDTO.builder()
                .id(1L)
                .date(LocalDate.now())
                .vehicleNo("KL 10 BE 1067")
                .brandName("Toyota")
                .modelName("Corolla")
                .variantName("Base")
                .saleRate(new BigDecimal("1450000"))
                .profit(new BigDecimal("15000"))
                .build());
        SalesRs salesRs = SalesRs.builder().sales(sales).build();
        salesRs.setTotalPages(1);
        salesRs.setTotalElements(2L);
        return salesRs;
    }
}
