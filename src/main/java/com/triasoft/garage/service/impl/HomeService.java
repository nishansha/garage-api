package com.triasoft.garage.service.impl;

import com.triasoft.garage.dto.ActivityDTO;
import com.triasoft.garage.dto.DataInfo;
import com.triasoft.garage.dto.SummaryInfo;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.home.ActivityRs;
import com.triasoft.garage.model.home.ChartRs;
import com.triasoft.garage.model.home.OverviewRs;
import com.triasoft.garage.model.home.SummaryRs;
import com.triasoft.garage.projection.ActivityProjection;
import com.triasoft.garage.projection.BalanceMetrics;
import com.triasoft.garage.projection.ProductMetrics;
import com.triasoft.garage.projection.SummaryMetrics;
import com.triasoft.garage.repository.SaleRepository;
import com.triasoft.garage.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final SaleRepository saleRepository;

    public SummaryRs summaryData(UserDTO user) {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        SummaryMetrics metrics = saleRepository.getFinancialSummary(startOfMonth);
        BigDecimal currentNetProfit = metrics.getTotalGrossProfit().subtract(metrics.getTotalExpenses());
        BigDecimal previousNetProfit = metrics.getGrossProfitBeforeMonth().subtract(metrics.getExpensesBeforeMonth());

        return SummaryRs.builder()
                .totalSales(String.valueOf(metrics.getTotalSales()))
                .salesDelta(CommonUtil.calculateDelta(metrics.getTotalSales(), metrics.getSalesBeforeMonth()))

                .totalPurchase(String.valueOf(metrics.getTotalPurchases()))
                .purchasesDelta(CommonUtil.calculateDelta(metrics.getTotalPurchases(), metrics.getPurchasesBeforeMonth()))

                .totalExpenses(String.valueOf(metrics.getTotalExpenses()))
                .expensesDelta(CommonUtil.calculateDelta(metrics.getTotalExpenses(), metrics.getExpensesBeforeMonth()))

                .totalProfit(String.valueOf(currentNetProfit))
                .profitDelta(CommonUtil.calculateDelta(currentNetProfit, previousNetProfit))
                .build();
    }

    public ChartRs getChartData(String type, UserDTO user) {
        List<ProductMetrics> topProducts;
        if (type.equalsIgnoreCase("PROFIT")) {
            topProducts = saleRepository.findTopProfitProducts();
        } else {
            topProducts = saleRepository.findTopSoldProducts();
        }
        List<DataInfo> dataInfoList = topProducts.stream().map(p -> DataInfo.builder().name(p.getName())
                .value(type.equalsIgnoreCase("PROFIT") ? String.valueOf(p.getRevenueValue()) : String.valueOf(p.getCountValue())).build()).toList();
        return ChartRs.builder().topProducts(dataInfoList).build();
    }

    public ActivityRs getActivities(UserDTO user) {
        List<ActivityProjection> activities = saleRepository.findRecentActivities();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy, hh:mm a");

        List<ActivityDTO> dataInfoList = activities.stream()
                .map(proj -> ActivityDTO.builder()
                        .activityType(proj.getActivityType())
                        .description(proj.getDescription())
                        .dateTime(proj.getDateTime().format(formatter))
                        .txnType(proj.getTxnType())
                        .txnAmount(proj.getTxnAmount().toPlainString())
                        .build())
                .toList();

        return ActivityRs.builder().activities(dataInfoList).build();
    }

    public OverviewRs getOverview(Integer monthCount, UserDTO user) {
        List<BalanceMetrics> results = saleRepository.getMonthlyBalanceSheet(monthCount);
        List<SummaryInfo> data = results.stream().map(m -> SummaryInfo.builder()
                .month(m.getMonthName())
                .totalSales(m.getSales())
                .totalPurchase(m.getPurchases())
                .totalExpenses(m.getExpenses())
                .totalProfit(m.getProfit())
                .build()
        ).toList();
        return OverviewRs.builder().data(data).build();
    }
}
