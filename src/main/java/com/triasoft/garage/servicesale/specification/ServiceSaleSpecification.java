package com.triasoft.garage.servicesale.specification;

import com.triasoft.garage.entity.Customer;
import com.triasoft.garage.model.common.FilterRq;
import com.triasoft.garage.servicesale.entity.ServiceSale;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ServiceSaleSpecification {

    public static Specification<ServiceSale> buildSearchQuery(FilterRq filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("saleDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("saleDate"), filter.getToDate()));
            }
            if (StringUtils.hasText(filter.getWarehouseId())) {
                predicates.add(cb.equal(root.get("warehouseId"), Long.valueOf(filter.getWarehouseId())));
            }
            if (StringUtils.hasText(filter.getSearchText())) {
                Join<ServiceSale, Customer> customer = root.join("customer", JoinType.LEFT);
                String pattern = "%" + filter.getSearchText().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("invoiceNo")), pattern),
                        cb.like(cb.lower(root.get("walkInCustomerName")), pattern),
                        cb.like(cb.lower(customer.get("name")), pattern),
                        cb.like(cb.lower(customer.get("mobile")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
