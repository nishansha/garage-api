package com.triasoft.garage.specifiction;

import com.triasoft.garage.entity.Customer;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.entity.Product;
import com.triasoft.garage.entity.Sale;
import com.triasoft.garage.model.common.FilterRq;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SaleSpecification {

    public static Specification<Sale> buildSearchQuery(FilterRq filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("saleDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("saleDate"), filter.getToDate()));
            }
            Join<Sale, Inventory> inventory = root.join("inventory", JoinType.INNER);
            Join<Inventory, Product> product = inventory.join("product", JoinType.INNER);

            if (filter.getBrandId() != null) {
                predicates.add(cb.equal(product.get("brand").get("id"), filter.getBrandId()));
            }
            if (filter.getModelId() != null) {
                predicates.add(cb.equal(product.get("model").get("id"), filter.getModelId()));
            }
            if (filter.getVariantId() != null) {
                predicates.add(cb.equal(product.get("varient").get("id"), filter.getVariantId()));
            }
            if (filter.getVehicleNo() != null && !filter.getVehicleNo().isBlank()) {
                predicates.add(cb.like(cb.lower(inventory.get("productNo")), "%" + filter.getVehicleNo().toLowerCase() + "%"));
            }

            if (filter.getSearchText() != null && !filter.getSearchText().isBlank()) {
                String pattern = "%" + filter.getSearchText().toLowerCase() + "%";
                Join<Sale, Customer> customer = root.join("customer", JoinType.INNER);

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("invoiceNo")), pattern),
                        cb.like(cb.lower(customer.get("name")), pattern),
                        cb.like(cb.lower(customer.get("mobileNo")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
