package com.triasoft.garage.servicesale.service;

import com.triasoft.garage.company.constants.BusinessLine;
import com.triasoft.garage.company.repository.WarehouseBusinessLineRepository;
import com.triasoft.garage.constants.ErrorCode;
import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.dto.UserDTO;
import com.triasoft.garage.entity.Customer;
import com.triasoft.garage.entity.PaymentAccount;
import com.triasoft.garage.entity.Warehouse;
import com.triasoft.garage.exception.BusinessException;
import com.triasoft.garage.locking.VersionCheck;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.repository.CustomerRepository;
import com.triasoft.garage.repository.PaymentAccountRepository;
import com.triasoft.garage.repository.WarehouseRepository;
import com.triasoft.garage.service.impl.JournalService;
import com.triasoft.garage.servicesale.dto.ServiceSaleDTO;
import com.triasoft.garage.servicesale.dto.ServiceSaleItemDTO;
import com.triasoft.garage.servicesale.dto.ServiceSalePaymentDTO;
import com.triasoft.garage.servicesale.entity.ServiceOffering;
import com.triasoft.garage.servicesale.entity.ServiceSale;
import com.triasoft.garage.servicesale.entity.ServiceSaleItem;
import com.triasoft.garage.servicesale.entity.ServiceSalePayment;
import com.triasoft.garage.servicesale.model.ServiceSaleItemRq;
import com.triasoft.garage.servicesale.model.ServiceSalePaymentRq;
import com.triasoft.garage.servicesale.model.ServiceSaleRq;
import com.triasoft.garage.servicesale.model.ServiceSaleRs;
import com.triasoft.garage.servicesale.repository.ServiceOfferingRepository;
import com.triasoft.garage.servicesale.repository.ServiceSalePaymentRepository;
import com.triasoft.garage.servicesale.repository.ServiceSaleRepository;
import com.triasoft.garage.servicesale.specification.ServiceSaleSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceSaleService {

    private final ServiceSaleRepository serviceSaleRepository;
    private final ServiceSalePaymentRepository serviceSalePaymentRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseBusinessLineRepository warehouseBusinessLineRepository;
    private final CustomerRepository customerRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final JournalService journalService;

    public ServiceSaleRs getAll(FilterRq filter, Pageable pageable) {
        Page<ServiceSale> page = serviceSaleRepository.findAll(ServiceSaleSpecification.buildSearchQuery(filter), pageable);
        ServiceSaleRs rs = ServiceSaleRs.builder().serviceSales(page.getContent().stream().map(this::toDTO).toList()).build();
        rs.setTotalPages(page.getTotalPages());
        rs.setTotalElements(page.getTotalElements());
        return rs;
    }

    public ServiceSaleDTO get(Long id) {
        return toDTO(findById(id));
    }

    @Transactional
    public ServiceSaleRs create(ServiceSaleRq rq, UserDTO user) {
        Warehouse warehouse = warehouseRepository.findById(rq.getWarehouseId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.WAREHOUSE_NOT_FOUND));
        if (!warehouseBusinessLineRepository.existsByWarehouseIdAndBusinessLine(warehouse.getId(), BusinessLine.SERVICES)) {
            throw new BusinessException(ErrorCode.Business.WAREHOUSE_BUSINESS_LINE_NOT_SUPPORTED);
        }

        ServiceSale sale = new ServiceSale();
        sale.setWarehouseId(warehouse.getId());
        sale.setCompanyId(warehouse.getCompanyId());
        sale.setInvoiceNo("SSO-" + serviceSaleRepository.getNextReferenceNumber());
        applyCustomer(sale, rq.getCustomerId(), rq.getWalkInCustomerName());
        sale.setSaleDate(rq.getSaleDate());
        sale.setNotes(rq.getNotes());
        sale.setItems(buildItems(sale, rq.getItems()));
        sale.setTotalAmount(sale.getItems().stream().map(ServiceSaleItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        sale.setPaymentStatus(StatusEnum.PENDING);
        serviceSaleRepository.save(sale);

        journalService.post(JournalService.REF_SERVICE_SALE, sale.getId());
        return ServiceSaleRs.builder().id(sale.getId()).build();
    }

    @Transactional
    @VersionCheck(entity = ServiceSale.class)
    public ServiceSaleRs update(Long id, ServiceSaleRq rq, UserDTO user) {
        ServiceSale sale = findById(id);
        journalService.reverse(JournalService.REF_SERVICE_SALE, id);

        // warehouse/company are immutable after creation — same reasoning as Purchase/Sale.
        applyCustomer(sale, rq.getCustomerId(), rq.getWalkInCustomerName());
        sale.setSaleDate(rq.getSaleDate());
        sale.setNotes(rq.getNotes());
        sale.getItems().clear();
        sale.getItems().addAll(buildItems(sale, rq.getItems()));
        sale.setTotalAmount(sale.getItems().stream().map(ServiceSaleItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal paid = serviceSalePaymentRepository.sumAmountByServiceSaleId(id);
        recalculatePaymentStatus(sale, paid);
        serviceSaleRepository.save(sale);

        journalService.post(JournalService.REF_SERVICE_SALE, id);
        return ServiceSaleRs.builder().id(sale.getId()).build();
    }

    @Transactional
    public void delete(Long id) {
        ServiceSale sale = findById(id);
        journalService.reverse(JournalService.REF_SERVICE_SALE, id);
        sale.getPayments().forEach(p -> journalService.reverseOnDate(JournalService.REF_SERVICE_SALE_PAYMENT, p.getId(), LocalDate.now()));
        serviceSaleRepository.delete(sale);
    }

    @Transactional
    public ServiceSaleRs recordPayment(Long serviceSaleId, ServiceSalePaymentRq rq) {
        ServiceSale sale = findById(serviceSaleId);
        PaymentAccount account = paymentAccountRepository.findById(rq.getPaymentAccountId())
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_ACCOUNT_NOT_FOUND));

        ServiceSalePayment payment = new ServiceSalePayment();
        payment.setServiceSale(sale);
        payment.setAmount(rq.getAmount());
        payment.setPaymentDate(rq.getPaymentDate());
        payment.setPaymentMethod(rq.getPaymentMethod());
        payment.setReferenceNo(rq.getReferenceNo());
        payment.setNotes(rq.getNotes());
        payment.setPaymentAccount(account);
        serviceSalePaymentRepository.save(payment);

        BigDecimal paid = serviceSalePaymentRepository.sumAmountByServiceSaleId(serviceSaleId);
        recalculatePaymentStatus(sale, paid);
        serviceSaleRepository.save(sale);

        journalService.post(JournalService.REF_SERVICE_SALE_PAYMENT, payment.getId());
        return ServiceSaleRs.builder().id(sale.getId()).build();
    }

    @Transactional
    public void deletePayment(Long serviceSaleId, Long paymentId) {
        ServiceSale sale = findById(serviceSaleId);
        ServiceSalePayment payment = serviceSalePaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.PAYMENT_NOT_FOUND));
        journalService.reverseOnDate(JournalService.REF_SERVICE_SALE_PAYMENT, paymentId, LocalDate.now());
        serviceSalePaymentRepository.delete(payment);
        BigDecimal paid = serviceSalePaymentRepository.sumAmountByServiceSaleId(serviceSaleId);
        recalculatePaymentStatus(sale, paid);
        serviceSaleRepository.save(sale);
    }

    private void applyCustomer(ServiceSale sale, Long customerId, String walkInCustomerName) {
        if (customerId != null) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.Business.CUSTOMER_NOT_FOUND));
            sale.setCustomer(customer);
            sale.setWalkInCustomerName(null);
        } else if (StringUtils.hasText(walkInCustomerName)) {
            sale.setCustomer(null);
            sale.setWalkInCustomerName(walkInCustomerName);
        } else {
            throw new BusinessException(ErrorCode.Business.SERVICE_SALE_CUSTOMER_REQUIRED);
        }
    }

    private List<ServiceSaleItem> buildItems(ServiceSale sale, List<ServiceSaleItemRq> itemRqs) {
        return itemRqs.stream().map(itemRq -> {
            ServiceSaleItem item = new ServiceSaleItem();
            item.setServiceSale(sale);
            if (itemRq.getServiceOfferingId() != null) {
                ServiceOffering offering = serviceOfferingRepository.findById(itemRq.getServiceOfferingId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.Business.SERVICE_OFFERING_NOT_FOUND));
                item.setServiceOffering(offering);
            }
            item.setDescription(itemRq.getDescription());
            item.setQty(itemRq.getQty());
            item.setRate(itemRq.getRate());
            item.setAmount(itemRq.getQty().multiply(itemRq.getRate()));
            return item;
        }).toList();
    }

    private void recalculatePaymentStatus(ServiceSale sale, BigDecimal totalPaid) {
        if (totalPaid.compareTo(sale.getTotalAmount()) >= 0) {
            sale.setPaymentStatus(StatusEnum.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            sale.setPaymentStatus(StatusEnum.PARTIAL);
        } else {
            sale.setPaymentStatus(StatusEnum.PENDING);
        }
    }

    private ServiceSale findById(Long id) {
        return serviceSaleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.Business.SERVICE_SALE_NOT_FOUND));
    }

    private ServiceSaleDTO toDTO(ServiceSale sale) {
        BigDecimal paid = serviceSalePaymentRepository.sumAmountByServiceSaleId(sale.getId());
        return ServiceSaleDTO.builder()
                .id(sale.getId())
                .version(sale.getVersion())
                .invoiceNo(sale.getInvoiceNo())
                .companyId(sale.getCompanyId())
                .warehouseId(sale.getWarehouseId())
                .customerId(sale.getCustomer() != null ? sale.getCustomer().getId() : null)
                .customerName(sale.customerDisplayName())
                .walkInCustomerName(sale.getWalkInCustomerName())
                .saleDate(sale.getSaleDate())
                .totalAmount(sale.getTotalAmount())
                .paidAmount(paid)
                .paymentStatus(sale.getPaymentStatus() != null ? sale.getPaymentStatus().name() : null)
                .notes(sale.getNotes())
                .items(sale.getItems().stream().map(this::toItemDTO).toList())
                .payments(sale.getPayments().stream().map(this::toPaymentDTO).toList())
                .build();
    }

    private ServiceSaleItemDTO toItemDTO(ServiceSaleItem item) {
        return ServiceSaleItemDTO.builder()
                .id(item.getId())
                .serviceOfferingId(item.getServiceOffering() != null ? item.getServiceOffering().getId() : null)
                .description(item.getDescription())
                .qty(item.getQty())
                .rate(item.getRate())
                .amount(item.getAmount())
                .build();
    }

    private ServiceSalePaymentDTO toPaymentDTO(ServiceSalePayment payment) {
        return ServiceSalePaymentDTO.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNo(payment.getReferenceNo())
                .notes(payment.getNotes())
                .paymentAccountId(payment.getPaymentAccount().getId())
                .paymentAccountName(payment.getPaymentAccount().getName())
                .build();
    }
}
