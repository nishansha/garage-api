package com.triasoft.garage.servicesale.entity;

import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.entity.Customer;
import com.triasoft.garage.entity.TenantAwareAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SoftDelete;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "app_service_sale")
@SoftDelete(columnName = "deleted")
public class ServiceSale extends TenantAwareAuditEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "invoice_no", unique = true, nullable = false)
    private String invoiceNo;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    // Nullable — a walk-in customer with no registered record uses walkInCustomerName
    // instead, same "no FK forced" idea as DirectEntry.partyName.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "walk_in_customer_name")
    private String walkInCustomerName;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private StatusEnum paymentStatus;

    @Column(name = "notes")
    private String notes;

    @OneToMany(mappedBy = "serviceSale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceSaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "serviceSale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceSalePayment> payments = new ArrayList<>();

    public String customerDisplayName() {
        return customer != null ? customer.getName() : walkInCustomerName;
    }
}
