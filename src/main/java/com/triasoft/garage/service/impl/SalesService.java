package com.triasoft.garage.service.impl;

import com.triasoft.garage.locking.VersionCheck;

import com.triasoft.garage.constants.*;
import com.triasoft.garage.dto.PurchaseDTO;
import com.triasoft.garage.dto.SaleAmountSplitDTO;
import com.triasoft.garage.dto.SaleDTO;
import com.triasoft.garage.dto.SalePaymentDTO;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.*;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.helper.LookupHelper;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.model.purchase.PurchaseRq;
import com.triasoft.garage.model.report.ReceivableInfo;
import com.triasoft.garage.model.report.ReceivablesSummaryRs;
import com.triasoft.garage.model.sale.SalePaymentRq;
import com.triasoft.garage.model.sale.SaleSummaryRs;
import com.triasoft.garage.model.sale.SalesRq;
import com.triasoft.garage.model.sale.SalesRs;
import com.triasoft.garage.projection.ReceivableRow;
import com.triasoft.garage.projection.SaleMetrics;
import com.triasoft.garage.repository.*;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final InventoryRepository inventoryRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final SaleAmountSplitRepository saleAmountSplitRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LookupHelper lookupHelper;
    private final PurchaseService purchaseService;
    private final JournalService journalService;

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
        return SaleDTO.builder().id(sale.getId()).version(sale.getVersion()).date(sale.getSaleDate())
                .customerName(sale.getCustomer().getName())
                .customerMobileNo(sale.getCustomer().getMobile())
                .vehicleNo(sale.getInventory().getProductNo())
                .brandName(sale.getInventory().getProduct().getBrand().getDescription())
                .modelName(sale.getInventory().getProduct().getModel().getDescription())
                .variantName(sale.getInventory().getProduct().getVarient().getDescription())
                .segmentId(sale.getInventory().getProduct().getSegment() != null ? sale.getInventory().getProduct().getSegment().getId() : null)
                .segmentName(sale.getInventory().getProduct().getSegment() != null ? sale.getInventory().getProduct().getSegment().getDescription() : null)
                .saleRate(sale.getSaleRate())
                .profit(sale.getProfitAmount())
                .isExchange(sale.isExchanged())
                .isFinanced(sale.isFinanced())
                .paymentStatus(sale.getPaymentStatus())
                .statusId(sale.getStatus() != null ? sale.getStatus().getId() : null)
                .statusName(sale.getStatus() != null ? sale.getStatus().getDescription() : null)
                .build();
    }

    @Transactional
    public SalesRs create(SalesRq saleRq, UserDTO user) {
        Customer customer = customerRepository.findByMobile(saleRq.getCustomerMobileNo()).orElseGet(() -> createCustomer(saleRq, user));
        Inventory stock = inventoryRepository.findById(saleRq.getStockId()).orElseThrow(() -> new EntityNotFoundException("Vehicle not found in stock"));
        if (!StatusEnum.AVAILABLE.equals(stock.getStatus())) {
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
        BigDecimal exchangeAmt = saleRq.isExchanged() && Objects.nonNull(saleRq.getExchangeAmount()) ? saleRq.getExchangeAmount() : BigDecimal.ZERO;
        sale.setExchangeAmount(exchangeAmt);
        sale.setNetSaleAmount(saleRq.getSaleRate().subtract(exchangeAmt));
        BigDecimal financeAmt = resolveFinanceAmount(saleRq, sale.getNetSaleAmount());
        sale.setFinanceAmount(financeAmt);
        sale.setFinanceCompany(saleRq.isFinanced() ? saleRq.getFinanceCompany() : null);
        sale.setEmiAmount(saleRq.isFinanced() ? saleRq.getEmiAmount() : null);
        sale.setLandedCostAtSale(stock.getLandedCost());
        sale.setProfitAmount(saleRq.getSaleRate().subtract(stock.getLandedCost()));
        sale.setStatus(saleRq.getStatusId() != null
                ? lookupHelper.get(saleRq.getStatusId())
                : lookupHelper.getStatus(LookupTypeEnum.SALES_STAUS, StatusEnum.COMPLETED));
        if (sale.getNetSaleAmount().compareTo(BigDecimal.ZERO) <= 0) {
            // Sale fully covered by exchange (or exchange exceeds sale — settlement tracked on trade-in Purchase).
            sale.setPaymentStatus(StatusEnum.PAID);
        } else if (saleRq.isFinanced()) {
            sale.setPaymentStatus(StatusEnum.FINANCE_PENDING);
        } else {
            sale.setPaymentStatus(StatusEnum.PENDING);
        }
        sale = saleRepository.save(sale);
        if (saleRq.isExchanged()) {
            handleExchangeVehicle(saleRq, sale, user);
        }
        stock.setStatus(StatusEnum.SOLD);
        inventoryRepository.save(stock);
        saveAmountSplits(sale, saleRq.getAmountSplits());
        journalService.post(JournalService.REF_SALE, sale.getId());
        return SalesRs.builder().id(sale.getId()).build();
    }

    private void handleExchangeVehicle(SalesRq saleRq, Sale sale, UserDTO user) {
        if (saleRq.getExchangeVehicleDetails() == null) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_400", "Exchange vehicle details are required when isExchanged is true"));
        }
        PurchaseDTO details = saleRq.getExchangeVehicleDetails();
        PurchaseRq exchangePurchaseRq = new PurchaseRq();
        exchangePurchaseRq.setCode(details.getCode());
        exchangePurchaseRq.setOwnerName(saleRq.getCustomerName());
        exchangePurchaseRq.setOwnerMobileNo(saleRq.getCustomerMobileNo());
        exchangePurchaseRq.setOwnerShipSerialNo(details.getOwnerShipSerialNo());
        exchangePurchaseRq.setVehicleNo(details.getVehicleNo());
        exchangePurchaseRq.setBrandId(details.getBrandId());
        exchangePurchaseRq.setModelId(details.getModelId());
        exchangePurchaseRq.setVariantId(details.getVariantId());
        exchangePurchaseRq.setSegmentId(details.getSegmentId());
        exchangePurchaseRq.setFuelTypeId(details.getFuelTypeId());
        exchangePurchaseRq.setTransmissionTypeId(details.getTransmissionTypeId());
        exchangePurchaseRq.setMakeYear(details.getMakeYear());
        exchangePurchaseRq.setOdometer(details.getOdometer());
        exchangePurchaseRq.setPurchaseRate(saleRq.getExchangeAmount());
        exchangePurchaseRq.setDate(saleRq.getDate());
        exchangePurchaseRq.setNotes("Garage Exchange");
        exchangePurchaseRq.setExpenses(details.getExpenses());
        exchangePurchaseRq.setSourceSaleId(sale.getId());
        exchangePurchaseRq.setDeliveredDate(Objects.isNull(details.getDeliveredDate()) ? saleRq.getDate() : details.getDeliveredDate());
        exchangePurchaseRq.setColorId(details.getColorId());
        purchaseService.create(exchangePurchaseRq, user);
    }

    private void handleUpdateExchangeVehicle(SalesRq saleRq, Sale sale, UserDTO user) {
        if (saleRq.getExchangeVehicleDetails() == null) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_400", "Exchange vehicle details are required when isExchanged is true"));
        }
        Inventory exchangeInv = inventoryRepository.findBySourceSaleId(sale.getId())
                .orElseThrow(() -> new EntityNotFoundException("Exchange inventory record not found for this sale"));

        PurchaseDTO details = saleRq.getExchangeVehicleDetails();
        PurchaseRq exchangePurchaseRq = new PurchaseRq();
        exchangePurchaseRq.setVersion(details.getVersion());
        exchangePurchaseRq.setCode(details.getCode());
        exchangePurchaseRq.setOwnerName(saleRq.getCustomerName());
        exchangePurchaseRq.setOwnerMobileNo(saleRq.getCustomerMobileNo());
        exchangePurchaseRq.setOwnerShipSerialNo(details.getOwnerShipSerialNo());
        exchangePurchaseRq.setVehicleNo(details.getVehicleNo());
        exchangePurchaseRq.setBrandId(details.getBrandId());
        exchangePurchaseRq.setModelId(details.getModelId());
        exchangePurchaseRq.setVariantId(details.getVariantId());
        exchangePurchaseRq.setSegmentId(details.getSegmentId());
        exchangePurchaseRq.setFuelTypeId(details.getFuelTypeId());
        exchangePurchaseRq.setTransmissionTypeId(details.getTransmissionTypeId());
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
        BigDecimal paidAmount = salePaymentRepository.sumAmountBySaleId(id);
        BigDecimal pending = existingSale.getNetSaleAmount().subtract(paidAmount);
        BigDecimal financePaid = salePaymentRepository.sumAmountBySaleIdAndPayerType(id, PayerTypeEnum.FINANCE);
        BigDecimal customerPaid = salePaymentRepository.sumAmountBySaleIdAndPayerType(id, PayerTypeEnum.CUSTOMER);
        BigDecimal financeAmount = existingSale.isFinanced() && existingSale.getFinanceAmount() != null
                ? existingSale.getFinanceAmount() : BigDecimal.ZERO;
        BigDecimal customerPayable = existingSale.getNetSaleAmount().subtract(financeAmount);
        SaleDTO dto = convertToDTO(existingSale);
        dto.setInvoiceNo(existingSale.getInvoiceNo());
        dto.setStockId(existingSale.getInventory().getId());
        dto.setNetSaleAmount(existingSale.getNetSaleAmount());
        dto.setPaymentStatus(existingSale.getPaymentStatus());
        dto.setExchangeAmount(existingSale.getExchangeAmount());
        dto.setFinanceCompany(existingSale.getFinanceCompany());
        dto.setFinanceAmount(existingSale.getFinanceAmount());
        dto.setEmiAmount(existingSale.getEmiAmount());
        dto.setPaidAmount(paidAmount);
        dto.setPendingAmount(pending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : pending);
        dto.setPaidFinanceAmount(financePaid);
        dto.setPendingFinanceAmount(existingSale.isFinanced()
                ? clampZero(financeAmount.subtract(financePaid)) : BigDecimal.ZERO);
        dto.setPaidCustomerAmount(customerPaid);
        dto.setPendingCustomerAmount(clampZero(customerPayable.subtract(customerPaid)));
        dto.setPayments(salePaymentRepository.findBySaleIdOrderByPaymentDateDesc(id)
                .stream().map(this::toPaymentDTO).toList());
        dto.setAmountSplits(saleAmountSplitRepository.findBySaleIdOrderByIdAsc(id)
                .stream().map(this::toAmountSplitDTO).toList());
        if (existingSale.isExchanged()) {
            inventoryRepository.findBySourceSaleId(id).ifPresent(exchangeInv -> {
                Long purchaseId = exchangeInv.getPurchaseOrderDetail().getPurchase().getId();
                dto.setExchangeVehicleDetails(purchaseService.get(purchaseId, user));
            });
        }
        return dto;
    }

    @Transactional
    @VersionCheck(entity = Sale.class)
    public SalesRs update(Long id, SalesRq salesRq, UserDTO user) {
        Sale existingSale = saleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        journalService.reverse(JournalService.REF_SALE, id);
        if (!existingSale.getInventory().getId().equals(salesRq.getStockId())) {
            Inventory newStock = inventoryRepository.findById(salesRq.getStockId()).orElseThrow(() -> new EntityNotFoundException("New stock item not found"));
            if (!StatusEnum.AVAILABLE.equals(newStock.getStatus())) {
                throw new BusinessException(ErrorCode.Business.ALREADY_SOLD);
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
        BigDecimal exchangeAmt = salesRq.isExchanged() && Objects.nonNull(salesRq.getExchangeAmount()) ? salesRq.getExchangeAmount() : BigDecimal.ZERO;
        BigDecimal newNetSaleAmount = salesRq.getSaleRate().subtract(exchangeAmt);
        BigDecimal newFinanceAmount = resolveFinanceAmount(salesRq, newNetSaleAmount);
        BigDecimal financePaidSoFar = salePaymentRepository.sumAmountBySaleIdAndPayerType(id, PayerTypeEnum.FINANCE);
        BigDecimal customerPaidSoFar = salePaymentRepository.sumAmountBySaleIdAndPayerType(id, PayerTypeEnum.CUSTOMER);
        if (!salesRq.isFinanced() && financePaidSoFar.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_412",
                    "Cannot disable finance: finance payments of " + financePaidSoFar + " already exist. Delete them first."));
        }
        if (salesRq.isFinanced() && financePaidSoFar.compareTo(newFinanceAmount) > 0) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_413",
                    "Finance amount cannot be lower than finance payments already recorded (" + financePaidSoFar + ")."));
        }
        BigDecimal newCustomerCap = newNetSaleAmount.subtract(newFinanceAmount != null ? newFinanceAmount : BigDecimal.ZERO);
        if (customerPaidSoFar.compareTo(newCustomerCap) > 0) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_414",
                    "Customer payments already recorded (" + customerPaidSoFar + ") exceed the new customer payable amount (" + newCustomerCap + ")."));
        }
        existingSale.setSaleDate(salesRq.getDate());
        existingSale.setExchanged(salesRq.isExchanged());
        existingSale.setSaleRate(salesRq.getSaleRate());
        existingSale.setExchangeAmount(exchangeAmt);
        existingSale.setFinanced(salesRq.isFinanced());
        existingSale.setFinanceCompany(salesRq.isFinanced() ? salesRq.getFinanceCompany() : null);
        existingSale.setFinanceAmount(newFinanceAmount);
        existingSale.setEmiAmount(salesRq.isFinanced() ? salesRq.getEmiAmount() : null);
        existingSale.setModifiedBy(user.getId());
        existingSale.setModifiedAt(LocalDateTime.now());
        existingSale.setNetSaleAmount(newNetSaleAmount);
        existingSale.setProfitAmount(salesRq.getSaleRate().subtract(existingSale.getLandedCostAtSale()));
        if (salesRq.getStatusId() != null) {
            existingSale.setStatus(lookupHelper.get(salesRq.getStatusId()));
        }
        if (existingSale.getNetSaleAmount().compareTo(BigDecimal.ZERO) <= 0) {
            // Sale fully covered by exchange (or exchange exceeds sale — settlement tracked on trade-in Purchase).
            existingSale.setPaymentStatus(StatusEnum.PAID);
        } else {
            recalculatePaymentStatus(existingSale, financePaidSoFar.add(customerPaidSoFar));
        }
        saleRepository.save(existingSale);
        syncAmountSplits(existingSale, salesRq.getAmountSplits());

        // TODO [JOURNAL ENTRY] - Sale Updated
        // Trigger  : after any sale field that affects amounts or vehicle changes.
        // Strategy : reverse the original sale journal entries, then post fresh ones.
        //   Reversal of Entry 1:  Dr Sales Revenue (Income)     / Cr Accounts Receivable (Asset)
        //   Reversal of Entry 2:  Dr Vehicle Inventory (Asset)  / Cr Cost of Goods Sold (Expense)
        //   New Entry 1:          Dr Accounts Receivable        / Cr Sales Revenue         (new saleRate)
        //   New Entry 2:          Dr Cost of Goods Sold         / Cr Vehicle Inventory     (new landedCostAtSale)
        // If vehicle changed: the COGS reversal must use the OLD vehicle's landedCostAtSale and the
        //   new entry must use the NEW vehicle's landedCostAtSale (already updated on existingSale above).
        // Future call: JournalEntryService.reverseByReference("SALE", id);
        //              JournalEntryService.postSale(id)

        journalService.post(JournalService.REF_SALE, id);
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
        purchaseService.deleteInternal(exchangeInv.getPurchaseOrderDetail().getPurchase().getId(), user);
    }

    @Transactional
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
                purchaseService.deleteInternal(exchangeInv.getPurchaseOrderDetail().getPurchase().getId(), user);
            });
        }
        salePaymentRepository.findBySaleIdOrderByPaymentDateDesc(id)
                .forEach(this::reverseSalePaymentTransaction);
        journalService.reverse(JournalService.REF_SALE, id);
        // TODO [JOURNAL ENTRY] - Sale Deleted
        // Trigger  : before the sale record is deleted.
        // Entry    : full reversal of both sale journal entries.
        //   Reversal of Revenue:  Dr Sales Revenue      (Income)   sale.getSaleRate()
        //                         Cr Accounts Receivable (Asset)    sale.getSaleRate()
        //   Reversal of COGS:     Dr Vehicle Inventory  (Asset)    sale.getLandedCostAtSale()
        //                         Cr Cost of Goods Sold (Expense)  sale.getLandedCostAtSale()
        // Future call: JournalEntryService.reverseByReference("SALE", id)
        // Note: Reversal must be posted BEFORE deletion so the reference ID still resolves.
        //       If the sale had a finance entry, that must also be reversed here.

        saleRepository.delete(sale);
        return SalesRs.builder().build();
    }

    @Transactional
    public SalesRs recordPayment(Long saleId, SalePaymentRq rq, UserDTO user) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        BigDecimal alreadyPaid = salePaymentRepository.sumAmountBySaleId(saleId);
        BigDecimal remaining = sale.getNetSaleAmount().subtract(alreadyPaid);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_400", "Sale is already fully paid"));
        }
        if (rq.getAmount().compareTo(remaining) > 0) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_401", "Payment amount exceeds the remaining balance for this sale"));
        }
        validatePayerBucket(sale, rq.getPayerType(), rq.getAmount(), BigDecimal.ZERO);
        PaymentAccount paymentAccount = resolvePaymentAccount(rq);
        SalePayment payment = new SalePayment();
        payment.setSale(sale);
        payment.setAmount(rq.getAmount());
        payment.setPaymentDate(rq.getPaymentDate() != null ? rq.getPaymentDate() : LocalDate.now());
        payment.setPaymentMethod(rq.getPaymentMethod());
        payment.setPayerType(rq.getPayerType());
        payment.setReferenceNo(rq.getReferenceNo());
        payment.setNotes(rq.getNotes());
        payment.setPaymentAccount(paymentAccount);
        SalePayment saved = salePaymentRepository.save(payment);
        createSaleTransaction(saved, sale.getInvoiceNo(), paymentAccount);
        recalculatePaymentStatus(sale, alreadyPaid.add(rq.getAmount()));
        saleRepository.save(sale);
        return SalesRs.builder().build();
    }

    @Transactional
    @VersionCheck(entity = SalePayment.class, idIndex = 1)
    public SalesRs updatePayment(Long saleId, Long paymentId, SalePaymentRq rq, UserDTO user) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        SalePayment payment = salePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(new ErrorCode.CustomError("SAL_404", "Payment record not found")));
        if (!payment.getSale().getId().equals(saleId)) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_404", "Payment record not found"));
        }
        boolean amountChanged = payment.getAmount().compareTo(rq.getAmount()) != 0;
        Long oldAccountId = payment.getPaymentAccount() != null ? payment.getPaymentAccount().getId() : null;
        boolean accountChanged = !Objects.equals(oldAccountId, rq.getPaymentAccountId());
        if (amountChanged || accountChanged) {
            BigDecimal alreadyPaid = salePaymentRepository.sumAmountBySaleId(saleId);
            BigDecimal paidExcludingThis = alreadyPaid.subtract(payment.getAmount());
            BigDecimal remaining = sale.getNetSaleAmount().subtract(paidExcludingThis);
            if (rq.getAmount().compareTo(remaining) > 0) {
                throw new BusinessException(new ErrorCode.CustomError("SAL_401", "Payment amount exceeds the remaining balance for this sale"));
            }
            reverseSalePaymentTransaction(payment);
        }
        BigDecimal excludeFromBucket = rq.getPayerType() == payment.getPayerType() ? payment.getAmount() : BigDecimal.ZERO;
        validatePayerBucket(sale, rq.getPayerType(), rq.getAmount(), excludeFromBucket);
        payment.setAmount(rq.getAmount());
        payment.setPaymentDate(rq.getPaymentDate() != null ? rq.getPaymentDate() : payment.getPaymentDate());
        payment.setPaymentMethod(rq.getPaymentMethod());
        payment.setPayerType(rq.getPayerType());
        payment.setReferenceNo(rq.getReferenceNo());
        payment.setNotes(rq.getNotes());
        PaymentAccount newAccount = resolvePaymentAccount(rq);
        payment.setPaymentAccount(newAccount);
        SalePayment saved = salePaymentRepository.save(payment);
        if (amountChanged || accountChanged) {
            createSaleTransaction(saved, sale.getInvoiceNo(), newAccount);
        }
        BigDecimal newTotal = salePaymentRepository.sumAmountBySaleId(saleId);
        recalculatePaymentStatus(sale, newTotal);
        saleRepository.save(sale);
        return SalesRs.builder().build();
    }

    @Transactional
    public SalesRs deletePayment(Long saleId, Long paymentId, UserDTO user) {
        SalePayment payment = salePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(new ErrorCode.CustomError("SAL_404", "Payment record not found")));
        if (!payment.getSale().getId().equals(saleId)) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_404", "Payment record not found"));
        }
        reverseSalePaymentTransaction(payment);
        salePaymentRepository.delete(payment);
        Sale sale = saleRepository.findById(saleId).orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        BigDecimal newTotal = salePaymentRepository.sumAmountBySaleId(saleId);
        recalculatePaymentStatus(sale, newTotal);
        saleRepository.save(sale);
        return SalesRs.builder().build();
    }

    private BigDecimal resolveFinanceAmount(SalesRq saleRq, BigDecimal netSaleAmount) {
        if (!saleRq.isFinanced()) {
            return null;
        }
        BigDecimal financeAmount = saleRq.getFinanceAmount();
        if (financeAmount == null || financeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_410",
                    "Finance amount is required and must be greater than zero for a financed sale."));
        }
        if (financeAmount.compareTo(netSaleAmount) > 0) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_411",
                    "Finance amount (" + financeAmount + ") cannot exceed net sale amount (" + netSaleAmount + ")."));
        }
        return financeAmount;
    }

    private void validatePayerBucket(Sale sale, PayerTypeEnum payerType, BigDecimal newAmount, BigDecimal excludeAmount) {
        if (payerType == PayerTypeEnum.FINANCE && !sale.isFinanced()) {
            throw new BusinessException(new ErrorCode.CustomError("SAL_420",
                    "Finance payment not allowed on a non-financed sale."));
        }
        BigDecimal cap = bucketCap(sale, payerType);
        BigDecimal bucketPaid = salePaymentRepository.sumAmountBySaleIdAndPayerType(sale.getId(), payerType)
                .subtract(excludeAmount);
        if (bucketPaid.add(newAmount).compareTo(cap) > 0) {
            String bucketLabel = payerType == PayerTypeEnum.FINANCE ? "finance" : "customer";
            throw new BusinessException(new ErrorCode.CustomError("SAL_421",
                    "Payment exceeds the " + bucketLabel + " payable amount (" + cap + "). "
                            + "Edit the sale to revise the finance amount if needed."));
        }
    }

    private BigDecimal bucketCap(Sale sale, PayerTypeEnum payerType) {
        BigDecimal financeAmount = sale.isFinanced() && sale.getFinanceAmount() != null
                ? sale.getFinanceAmount() : BigDecimal.ZERO;
        return payerType == PayerTypeEnum.FINANCE
                ? financeAmount
                : sale.getNetSaleAmount().subtract(financeAmount);
    }

    private BigDecimal clampZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private PaymentAccount resolvePaymentAccount(SalePaymentRq rq) {
        if (rq.getPaymentAccountId() == null) {
            if (PaymentMethodEnum.BANK.equals(rq.getPaymentMethod()) || PaymentMethodEnum.CHEQUE.equals(rq.getPaymentMethod())) {
                throw new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_REQUIRED);
            }
            return null;
        }
        return paymentAccountRepository.findById(rq.getPaymentAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));
    }

    private void createSaleTransaction(SalePayment payment, String invoiceNo, PaymentAccount paymentAccount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionDate(payment.getPaymentDate());
        transaction.setType(TransactionTypeEnum.SALE_RECEIPT);
        transaction.setReferenceType("SALE_PAYMENT");
        transaction.setReferenceId(payment.getId());
        transaction.setPaymentAccount(paymentAccount);
        transaction.setAmount(payment.getAmount());
        transaction.setDirection(TransactionDirectionEnum.IN);
        transaction.setDescription("Sale receipt – " + invoiceNo + " [" + payment.getPayerType().name() + "]");
        transaction.setNotes(payment.getNotes());
        transactionRepository.save(transaction);
        journalService.post(JournalService.REF_SALE_PAYMENT, payment.getId());

        // TODO [JOURNAL ENTRY] - Sale Payment Received
        // Trigger  : every time a payment is recorded against a sale (this is the single entry point).
        // Entry    : settles outstanding Accounts Receivable into the cash/bank account.
        //   Dr  <paymentAccount.name>    (Asset – Bank/Cash)    payment.getAmount()  (money arrives)
        //   Cr  Accounts Receivable      (Asset – Current)      payment.getAmount()  (reduces what customer owes)
        // Note: payerType distinguishes the source:
        //   CUSTOMER → standard customer payment (cash/cheque/bank)
        //   FINANCE  → disbursement from finance company; Cr goes against Finance Receivable, not AR
        //              Dr  <paymentAccount.name>    (Asset)             payment.getAmount()
        //              Cr  Finance Receivable        (Asset)            payment.getAmount()
        // paymentAccount may be null for CASH payments — Cr goes to "Cash in Hand" CoA account.
        // Future call: JournalEntryService.postSaleReceipt(transaction.getId())
        // CoA required: "Accounts Receivable" (Asset), "Finance Receivable" (Asset, for FINANCE payer),
        //               PaymentAccount's CoA entry (Asset – Bank/Cash).
    }

    private void reverseSalePaymentTransaction(SalePayment payment) {
        transactionRepository.findActiveByReferenceTypeAndReferenceId("SALE_PAYMENT", payment.getId())
                .ifPresent(original -> {
                    if (transactionRepository.existsByReversalOfId(original.getId())) return;
                    Transaction reversal = new Transaction();
                    reversal.setTransactionDate(LocalDate.now());
                    reversal.setType(TransactionTypeEnum.SALE_RECEIPT);
                    reversal.setReferenceType("SALE_PAYMENT");
                    reversal.setReferenceId(payment.getId());
                    reversal.setPaymentAccount(original.getPaymentAccount());
                    reversal.setAmount(original.getAmount());
                    reversal.setDirection(TransactionDirectionEnum.OUT);
                    reversal.setDescription("Reversal – " + original.getDescription());
                    reversal.setReversalOf(original);
                    transactionRepository.save(reversal);
                    journalService.reverseOnDate(JournalService.REF_SALE_PAYMENT, payment.getId(), LocalDate.now());

                    // TODO [JOURNAL ENTRY] - Sale Payment Reversal
                    // Trigger  : called on payment update (amount/account changed) or payment delete.
                    // Entry    : exact mirror of the original receipt entry, with Dr/Cr swapped.
                    //   CUSTOMER payment reversal:
                    //     Dr  Accounts Receivable      (Asset)             original.getAmount()
                    //     Cr  <paymentAccount.name>    (Asset – Bank/Cash) original.getAmount()
                    //   FINANCE payment reversal:
                    //     Dr  Finance Receivable        (Asset)            original.getAmount()
                    //     Cr  <paymentAccount.name>    (Asset – Bank/Cash) original.getAmount()
                    // Future call: JournalEntryService.reverseByTransactionId(original.getId())
                });
    }

    private void recalculatePaymentStatus(Sale sale, BigDecimal totalPaid) {
        if (totalPaid.compareTo(sale.getNetSaleAmount()) >= 0) {
            sale.setPaymentStatus(StatusEnum.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            sale.setPaymentStatus(StatusEnum.PARTIAL);
        } else if (sale.isFinanced()) {
            sale.setPaymentStatus(StatusEnum.FINANCE_PENDING);
        } else {
            sale.setPaymentStatus(StatusEnum.PENDING);
        }
    }

    private void saveAmountSplits(Sale sale, List<SaleAmountSplitDTO> splits) {
        if (splits == null || splits.isEmpty()) return;
        List<SaleAmountSplit> entities = new ArrayList<>();
        for (SaleAmountSplitDTO dto : splits) {
            SaleAmountSplit s = new SaleAmountSplit();
            s.setSale(sale);
            s.setType(lookupHelper.get(dto.getTypeId()));
            s.setAmount(dto.getAmount());
            entities.add(s);
        }
        saleAmountSplitRepository.saveAll(entities);
    }

    private void syncAmountSplits(Sale sale, List<SaleAmountSplitDTO> incoming) {
        List<SaleAmountSplit> existing = saleAmountSplitRepository.findBySaleIdOrderByIdAsc(sale.getId());
        Map<Long, SaleAmountSplit> byId = new HashMap<>();
        for (SaleAmountSplit s : existing) {
            byId.put(s.getId(), s);
        }
        Set<Long> keepIds = new HashSet<>();
        List<SaleAmountSplit> toSave = new ArrayList<>();
        if (incoming != null) {
            for (SaleAmountSplitDTO dto : incoming) {
                SaleAmountSplit s;
                if (dto.getId() != null && byId.containsKey(dto.getId())) {
                    s = byId.get(dto.getId());
                    keepIds.add(dto.getId());
                } else {
                    s = new SaleAmountSplit();
                    s.setSale(sale);
                }
                s.setType(lookupHelper.get(dto.getTypeId()));
                s.setAmount(dto.getAmount());
                toSave.add(s);
            }
        }
        List<SaleAmountSplit> toDelete = existing.stream()
                .filter(s -> !keepIds.contains(s.getId()))
                .toList();
        if (!toDelete.isEmpty()) saleAmountSplitRepository.deleteAll(toDelete);
        if (!toSave.isEmpty()) saleAmountSplitRepository.saveAll(toSave);
    }

    private SaleAmountSplitDTO toAmountSplitDTO(SaleAmountSplit s) {
        return SaleAmountSplitDTO.builder()
                .id(s.getId())
                .typeId(s.getType().getId())
                .typeDesc(s.getType().getDescription())
                .amount(s.getAmount())
                .build();
    }

    private SalePaymentDTO toPaymentDTO(SalePayment p) {
        return SalePaymentDTO.builder()
                .id(p.getId())
                .version(p.getVersion())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod())
                .payerType(p.getPayerType())
                .paymentAccountId(p.getPaymentAccount() != null ? p.getPaymentAccount().getId() : null)
                .paymentAccountName(p.getPaymentAccount() != null ? p.getPaymentAccount().getName() : null)
                .referenceNo(p.getReferenceNo())
                .notes(p.getNotes())
                .build();
    }

    public ReceivablesSummaryRs getReceivablesSummary() {
        List<ReceivableRow> rows = saleRepository.findReceivables();
        List<ReceivableInfo> items = rows.stream().map(r -> ReceivableInfo.builder()
                .saleId(r.getSaleId())
                .invoiceNo(r.getInvoiceNo())
                .paymentStatus(r.getPaymentStatus())
                .vehicleNo(r.getVehicleNo())
                .saleDate(r.getSaleDate())
                .amount(safe(r.getAmount()))
                .pendingAmount(safe(r.getPendingAmount()))
                .lastPaymentDate(r.getLastPaymentDate())
                .customerName(r.getCustomerName())
                .customerMobile(r.getCustomerMobile())
                .build()).toList();
        BigDecimal totalPending = items.stream()
                .map(ReceivableInfo::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ReceivablesSummaryRs.builder()
                .totalCount(items.size())
                .totalPendingAmount(totalPending)
                .items(items)
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
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
