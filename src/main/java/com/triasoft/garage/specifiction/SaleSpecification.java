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
            boolean isCountQuery = Long.class.equals(query.getResultType()) || long.class.equals(query.getResultType());
            Join<Sale, Inventory> inventory;
            Join<Inventory, Product> product;
            Join<Sale, Customer> customer;
            if (isCountQuery) {
                inventory = root.join("inventory", JoinType.INNER);
                product = inventory.join("product", JoinType.INNER);
                customer = root.join("customer", JoinType.INNER);
            } else {
                // Fetch (not just join) so the listing avoids per-row lazy/eager N+1 selects on these associations.
                // Hibernate's Fetch implementation also implements Join for *ToOne attributes; javac doesn't know
                // that, so the cast has to go through Object.
                inventory = (Join<Sale, Inventory>) (Object) root.fetch("inventory", JoinType.INNER);
                inventory.fetch("color", JoinType.LEFT);
                product = (Join<Inventory, Product>) (Object) inventory.fetch("product", JoinType.INNER);
                product.fetch("brand", JoinType.LEFT);
                product.fetch("model", JoinType.LEFT);
                product.fetch("varient", JoinType.LEFT);
                product.fetch("segment", JoinType.LEFT);
                product.fetch("fuelType", JoinType.LEFT);
                product.fetch("transmissionType", JoinType.LEFT);
                root.fetch("status", JoinType.INNER);
                customer = (Join<Sale, Customer>) (Object) root.fetch("customer", JoinType.INNER);
            }

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
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("invoiceNo")), pattern),
                        cb.like(cb.lower(customer.get("name")), pattern),
                        cb.like(cb.lower(customer.get("mobile")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
