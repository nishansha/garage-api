package com.triasoft.garage.specifiction;

import com.triasoft.garage.constants.StatusEnum;
import com.triasoft.garage.entity.Inventory;
import com.triasoft.garage.entity.Product;
import com.triasoft.garage.model.common.FilterRq;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class StockSpecification {

    public static Specification<Inventory> buildSearchQuery(FilterRq filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Date Range (Based on when the vehicle was received in stock)
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receivedDate"), filter.getFromDate().atStartOfDay()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receivedDate"), filter.getToDate().atTime(23, 59, 59)));
            }
            Join<Inventory, Product> product = root.join("product", JoinType.INNER);
            if (filter.getBrandId() != null) {
                predicates.add(cb.equal(product.get("brand").get("id"), filter.getBrandId()));
            }
            if (filter.getModelId() != null) {
                predicates.add(cb.equal(product.get("model").get("id"), filter.getModelId()));
            }
            if (filter.getVariantId() != null) {
                predicates.add(cb.equal(product.get("varient").get("id"), filter.getVariantId()));
            }
            if (filter.getFuelTypeId() != null) {
                predicates.add(cb.equal(product.get("fuelType").get("id"), filter.getFuelTypeId()));
            }
            if (filter.getVehicleNo() != null && !filter.getVehicleNo().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("productNo")), "%" + filter.getVehicleNo().toLowerCase() + "%"));
            }
            if (filter.getSearchText() != null && !filter.getSearchText().isBlank()) {
                String pattern = "%" + filter.getSearchText().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("productNo")), pattern),
                        cb.like(cb.lower(root.get("uin")), pattern),
                        cb.like(cb.lower(product.get("brand").get("description")), pattern),
                        cb.like(cb.lower(product.get("model").get("description")), pattern),
                        cb.like(cb.lower(product.get("varient").get("description")), pattern)
                ));
            }
            if (StringUtils.hasLength(filter.getStatus())) {
                predicates.add(cb.equal(root.get("status"), StatusEnum.valueOf(filter.getStatus())));
            } else {
                predicates.add(cb.or(
                        cb.equal(root.get("status"), StatusEnum.AVAILABLE),
                        cb.equal(root.get("status"), StatusEnum.PENDING_DELIVERY))
                );
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
