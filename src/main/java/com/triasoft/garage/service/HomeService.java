package com.triasoft.garage.service;

import com.triasoft.garage.dto.ActivityDTO;
import com.triasoft.garage.dto.DataInfo;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.model.home.ActivityRs;
import com.triasoft.garage.model.home.ChartRs;
import com.triasoft.garage.model.home.SummaryRs;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HomeService {

    public SummaryRs summaryData(UserDTO user) {
        return SummaryRs.builder().totalExpenses("0").totalProfit("0").totalSales("0").totalPurchase("0").build();
    }

    public ChartRs getChartData(UserDTO user) {
        List<DataInfo> dataInfoList = new ArrayList<>();
        dataInfoList.add(DataInfo.builder().name("toyota").value("10").build());
        dataInfoList.add(DataInfo.builder().name("maruti").value("5").build());
        return ChartRs.builder().topProducts(dataInfoList).build();
    }

    public ActivityRs getActivities(UserDTO user) {
        List<ActivityDTO> dataInfoList = new ArrayList<>();
        dataInfoList.add(ActivityDTO.builder()
                .activityType("SALE")
                .description("Toyota Corolla sale")
                .dateTime("11-2029, 11:12 PM")
                .txnType("C")
                .txnAmount("14000").build());

        dataInfoList.add(ActivityDTO.builder()
                .activityType("PURCHASE")
                .description("Honda City Purchase")
                .dateTime("11-2029, 11:12 PM")
                .txnType("D")
                .txnAmount("100000").build());

        dataInfoList.add(ActivityDTO.builder()
                .activityType("EXPENSE")
                .description("Office Rent")
                .dateTime("11-2029, 11:12 PM")
                .txnType("D")
                .txnAmount("180").build());
        return ActivityRs.builder().activities(dataInfoList).build();
    }
}
