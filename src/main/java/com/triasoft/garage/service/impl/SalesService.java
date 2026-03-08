package com.triasoft.garage.service.impl;

import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.LookupTypeEnum;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.dto.SaleDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.Customer;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.purchase.PurchaseRq;
import com.triasoft.garage.model.sale.SaleSummaryRs;
import com.triasoft.garage.model.sale.SalesRq;
import com.triasoft.garage.model.sale.SalesRs;
import com.triasoft.garage.projection.SaleMetrics;
import com.triasoft.garage.repository.CustomerRepository;
import com.triasoft.garage.repository.InventoryRepository;
import com.triasoft.garage.repository.SaleRepository;
import com.triasoft.garage.specifiction.SaleSpecification;
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
public class SalesService {

    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final LookupHelper lookupHelper;
    private final PurchaseService purchaseService;

    public SalesRs getAll(Pageable pageable, UserDTO user) {
        Page<Sale> salePage = saleRepository.findAll(pageable);
        List<SaleDTO> sales = salePage.getContent().stream().map(this::convertToDTO).toList();
        SalesRs salesRs = SalesRs.builder().sales(sales).build();
        salesRs.setTotalPages(salePage.getTotalPages());
        salesRs.setTotalElements(salePage.getTotalElements());
        return salesRs;
    }

    public SaleSummaryRs summary(UserDTO user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        LocalDate endOfLastMonth = startOfMonth.minusDays(1);

        SaleMetrics metrics = saleRepository.getSalesSummaryMetrics(startOfLastMonth, endOfLastMonth, startOfMonth, today);
        double monthRate = CommonUtil.calculateDelta(metrics.getTotalSalesThisMonth(), metrics.getTotalSalesLastMonth());
        return SaleSummaryRs.builder()
                .totalThisMonth(metrics.getTotalSalesThisMonth().toString())
                .monthRate(monthRate)
                .totalCount(metrics.getMonthCount())
                .todayCount(metrics.getTodayCount())
                .build();
    }

    public SalesRs search(FilterRq filterRq, Pageable pageable, UserDTO user) {
        Specification<Sale> spec = SaleSpecification.buildSearchQuery(filterRq);
        Page<Sale> salePage = saleRepository.findAll(spec, pageable);
        List<SaleDTO> dtoList = salePage.getContent().stream()
                .map(this::convertToDTO)
                .toList();
        SalesRs salesRs = SalesRs.builder().sales(dtoList).build();
        salesRs.setTotalPages(salePage.getTotalPages());
        salesRs.setTotalElements(salePage.getTotalElements());
        return salesRs;
    }

    private SaleDTO convertToDTO(Sale sale) {
        return SaleDTO.builder().id(sale.getId()).date(sale.getSaleDate())
                .vehicleNo(sale.getInventory().getProductNo())
                .brandName(sale.getInventory().getProduct().getBrand().getDescription())
                .modelName(sale.getInventory().getProduct().getModel().getDescription())
                .variantName(sale.getInventory().getProduct().getVarient().getDescription())
                .saleRate(sale.getSaleRate())
                .profit(sale.getProfitAmount())
                .build();
    }

    @Transactional
    public SalesRs create(SalesRq saleRq, UserDTO user) {
        Customer customer = customerRepository.findByMobile(saleRq.getCustomerMobileNo()).orElseGet(() -> createCustomer(saleRq, user));
        Inventory stock = inventoryRepository.findById(saleRq.getStockId()).orElseThrow(() -> new EntityNotFoundException("Vehicle not found in stock"));
        if (StatusEnum.SOLD.equals(stock.getStatus())) {
            throw new BusinessException(ErrorCode.Business.ALREADY_SOLD);
        }
        Sale sale = new Sale();
        sale.setInvoiceNo("SO-" + saleRepository.getNextReferenceNumber());
        sale.setCustomer(customer);
        sale.setInventory(stock);
        sale.setSaleDate(saleRq.getDate());
        sale.setSaleRate(saleRq.getSaleRate());
        sale.setExchanged(saleRq.isExchanged());
        sale.setFinanced(saleRq.isFinanced());
        sale.setFinanceAmount(saleRq.getFinanceAmount());
        sale.setFinanceCompany(saleRq.getFinanceCompany());
        BigDecimal exchangeAmt = Objects.nonNull(saleRq.getExchangeAmount()) ? saleRq.getExchangeAmount() : BigDecimal.ZERO;
        sale.setExchangeAmount(exchangeAmt);
        sale.setNetSaleAmount(saleRq.getSaleRate().subtract(exchangeAmt));
        sale.setLandedCostAtSale(stock.getLandedCost());
        sale.setProfitAmount(saleRq.getSaleRate().subtract(stock.getLandedCost()));
        sale.setStatus(lookupHelper.getStatus(LookupTypeEnum.SALES_STAUS, StatusEnum.COMPLETED));
        if (sale.getNetSaleAmount().compareTo(BigDecimal.ZERO) < 0) {
            sale.setPaymentStatus(StatusEnum.REFUND);
        } else if (sale.getNetSaleAmount().compareTo(BigDecimal.ZERO) == 0) {
            sale.setPaymentStatus(StatusEnum.PAID);
        } else {
            sale.setPaymentStatus(StatusEnum.PENDING);
        }
        sale = saleRepository.save(sale);
        if (saleRq.isExchanged()) {
            handleExchangeVehicle(saleRq, sale, user);
        }
        stock.setStatus(StatusEnum.SOLD);
        inventoryRepository.save(stock);
        return SalesRs.builder().build();
    }

    private void handleExchangeVehicle(SalesRq saleRq, Sale sale, UserDTO user) {
        PurchaseDTO details = saleRq.getExchangeVehicleDetails();
        PurchaseRq exchangePurchaseRq = new PurchaseRq();
        exchangePurchaseRq.setOwnerName(saleRq.getCustomerName());
        exchangePurchaseRq.setOwnerMobileNo(saleRq.getCustomerMobileNo());
        exchangePurchaseRq.setOwnerShipSerialNo(details.getOwnerShipSerialNo());
        exchangePurchaseRq.setVehicleNo(details.getVehicleNo());
        exchangePurchaseRq.setBrandId(details.getBrandId());
        exchangePurchaseRq.setModelId(details.getModelId());
        exchangePurchaseRq.setVariantId(details.getVariantId());
        exchangePurchaseRq.setMakeYear(details.getMakeYear());
        exchangePurchaseRq.setOdometer(details.getOdometer());
        exchangePurchaseRq.setPurchaseRate(saleRq.getExchangeAmount());
        exchangePurchaseRq.setDate(saleRq.getDate());
        exchangePurchaseRq.setNotes("Garage Exchange");
        exchangePurchaseRq.setExpenses(details.getExpenses());
        exchangePurchaseRq.setSourceSaleId(sale.getId());
        purchaseService.create(exchangePurchaseRq, user);
    }

    private void handleUpdateExchangeVehicle(SalesRq saleRq, Sale sale, UserDTO user) {
        Inventory exchangeInv = inventoryRepository.findBySourceSaleId(sale.getId())
                .orElseThrow(() -> new EntityNotFoundException("Exchange inventory record not found for this sale"));

        PurchaseDTO details = saleRq.getExchangeVehicleDetails();
        PurchaseRq exchangePurchaseRq = new PurchaseRq();
        exchangePurchaseRq.setOwnerName(saleRq.getCustomerName());
        exchangePurchaseRq.setOwnerMobileNo(saleRq.getCustomerMobileNo());
        exchangePurchaseRq.setOwnerShipSerialNo(details.getOwnerShipSerialNo());
        exchangePurchaseRq.setVehicleNo(details.getVehicleNo());
        exchangePurchaseRq.setBrandId(details.getBrandId());
        exchangePurchaseRq.setModelId(details.getModelId());
        exchangePurchaseRq.setVariantId(details.getVariantId());
        exchangePurchaseRq.setMakeYear(details.getMakeYear());
        exchangePurchaseRq.setOdometer(details.getOdometer());
        exchangePurchaseRq.setPurchaseRate(saleRq.getExchangeAmount());
        exchangePurchaseRq.setDate(saleRq.getDate());
        exchangePurchaseRq.setExpenses(details.getExpenses());
        purchaseService.update(exchangeInv.getPurchaseOrderDetail().getPurchase().getId(), exchangePurchaseRq, user);
    }

    private Customer createCustomer(SalesRq saleRq, UserDTO user) {
        Customer customer = new Customer();
        customer.setName(saleRq.getCustomerName());
        customer.setMobile(saleRq.getCustomerMobileNo());
        customer.setAddress(saleRq.getCustomerAddress());
        return customerRepository.save(customer);
    }

    public SaleDTO get(Long id, UserDTO user) {
        Sale existingSale = saleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        return convertToDTO(existingSale);
    }

    @Transactional
    public SalesRs update(Long id, SalesRq salesRq, UserDTO user) {
        Sale existingSale = saleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        if (!existingSale.getInventory().getId().equals(salesRq.getStockId())) {
            Inventory newStock = inventoryRepository.findById(salesRq.getStockId()).orElseThrow(() -> new EntityNotFoundException("New stock item not found"));
            if (StatusEnum.SOLD.equals(newStock.getStatus())) {
                throw new BusinessException("Selected vehicle is already sold to someone else!");
            }
            Inventory oldStock = existingSale.getInventory();
            oldStock.setStatus(StatusEnum.AVAILABLE);
            inventoryRepository.save(oldStock);

            newStock.setStatus(StatusEnum.SOLD);
            existingSale.setInventory(newStock);
            existingSale.setLandedCostAtSale(newStock.getLandedCost());
        }
        if (!existingSale.getCustomer().getMobile().equals(salesRq.getCustomerMobileNo())) {
            Customer newCustomer = customerRepository.findByMobile(salesRq.getCustomerMobileNo()).orElseGet(() -> updateCustomer(salesRq, user));
            existingSale.setCustomer(newCustomer);
        }
        syncExchangeVehicle(existingSale, salesRq, user);
        BigDecimal exchangeAmt = salesRq.isExchanged() ? salesRq.getExchangeAmount() : BigDecimal.ZERO;
        existingSale.setSaleRate(salesRq.getSaleRate());
        existingSale.setExchangeAmount(exchangeAmt);
        existingSale.setFinanced(salesRq.isFinanced());
        existingSale.setFinanceCompany(salesRq.getFinanceCompany());
        existingSale.setFinanceAmount(salesRq.getFinanceAmount());
        existingSale.setEmiAmount(salesRq.getEmiAmount());
        existingSale.setModifiedBy(user.getId());
        existingSale.setModifiedAt(LocalDateTime.now());
        existingSale.setNetSaleAmount(salesRq.getSaleRate().subtract(exchangeAmt));
        existingSale.setProfitAmount(salesRq.getSaleRate().subtract(existingSale.getLandedCostAtSale()));
        if (existingSale.getNetSaleAmount().compareTo(BigDecimal.ZERO) < 0) {
            existingSale.setPaymentStatus(StatusEnum.REFUND);
        } else if (existingSale.getNetSaleAmount().compareTo(BigDecimal.ZERO) == 0) {
            existingSale.setPaymentStatus(StatusEnum.PAID);
        } else {
            existingSale.setPaymentStatus(StatusEnum.PENDING);
        }
        saleRepository.save(existingSale);
        return SalesRs.builder().build();
    }

    private Customer updateCustomer(SalesRq salesRq, UserDTO user) {
        Customer customer = customerRepository.findByMobile(salesRq.getCustomerMobileNo()).orElseGet(() -> createCustomer(salesRq, user));
        if (!customer.getName().equalsIgnoreCase(salesRq.getCustomerName())) {
            customer.setName(salesRq.getCustomerName());
            customerRepository.save(customer);
        }
        return customer;
    }

    private void syncExchangeVehicle(Sale existingSale, SalesRq saleRq, UserDTO user) {
        boolean wasExchanged = existingSale.isExchanged();
        boolean isNowExchanged = saleRq.isExchanged();
        if (!wasExchanged && isNowExchanged) {
            handleExchangeVehicle(saleRq, existingSale, user);
        } else if (wasExchanged && !isNowExchanged) {
            removeOldExchangeVehicle(existingSale, user);
        } else if (wasExchanged && isNowExchanged) {
            handleUpdateExchangeVehicle(saleRq, existingSale, user);
        }
    }

    private void removeOldExchangeVehicle(Sale sale, UserDTO user) {
        Inventory exchangeInv = inventoryRepository.findBySourceSaleId(sale.getId())
                .orElseThrow(() -> new EntityNotFoundException("Exchange inventory record not found for this sale"));
        if (StatusEnum.SOLD.equals(exchangeInv.getStatus())) {
            throw new BusinessException("Cannot edit exchange details: the trade-in vehicle has already been sold!");
        }
        purchaseService.delete(exchangeInv.getPurchaseOrderDetail().getPurchase().getId(), user);
    }

    public SalesRs delete(Long id, UserDTO user) {
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        Inventory soldVehicle = sale.getInventory();
        soldVehicle.setStatus(StatusEnum.AVAILABLE);
        inventoryRepository.save(soldVehicle);
        if (sale.isExchanged()) {
            inventoryRepository.findBySourceSaleId(sale.getId()).ifPresent(exchangeInv -> {
                if (StatusEnum.SOLD.equals(exchangeInv.getStatus())) {
                    throw new BusinessException("Cannot delete sale: The exchange vehicle has already been sold to another customer!");
                }
                purchaseService.delete(exchangeInv.getPurchaseOrderDetail().getPurchase().getId(), user);
            });
        }
        saleRepository.delete(sale);
        return SalesRs.builder().build();
    }

//    public SaleSummaryRs getMonthlyProfitReport(UserDTO user) {
//        LocalDate today = LocalDate.now();
//        LocalDate startOfMonth = today.withDayOfMonth(1);
//
//        ProfitMetrics report = saleRepository.getProfitReport(startOfMonth, today);
//
//        // Calculate Margin Percentage: (Profit / Sales) * 100
//        double marginPercentage = 0.0;
//        if (report.getTotalSales().compareTo(BigDecimal.ZERO) > 0) {
//            marginPercentage = report.getNetProfit()
//                    .divide(report.getTotalSales(), 4, RoundingMode.HALF_UP)
//                    .multiply(BigDecimal.valueOf(100))
//                    .doubleValue();
//        }
//
//        return SaleSummaryRs.builder()
//                .totalRevenue(report.getTotalSales())
//                .totalProfit(report.getNetProfit())
//                .marginPercent(marginPercentage)
//                .unitsSold(report.getUnitsSold())
//                .build();
//    }
}
