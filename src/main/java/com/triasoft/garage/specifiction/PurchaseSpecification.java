package com.triasoft.garage.specifiction;

import com.triasoft.garage.entity.Product;
import com.triasoft.garage.entity.Purchase;
import com.triasoft.garage.entity.PurchaseDetail;
import com.triasoft.garage.model.common.FilterRq;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PurchaseSpecification {

    public static Specification<Purchase> buildSearchQuery(FilterRq filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), filter.getToDate()));
            }
            Join<Purchase, PurchaseDetail> details = root.join("purchaseDetails", JoinType.INNER);
            Join<PurchaseDetail, Product> product = details.join("product", JoinType.INNER);

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
                predicates.add(cb.like(cb.lower(details.get("vehicleNo")), "%" + filter.getVehicleNo().toLowerCase() + "%"));
            }
            if (filter.getSearchText() != null && !filter.getSearchText().isBlank()) {
                String pattern = "%" + filter.getSearchText().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("referenceNo")), pattern),
                        cb.like(cb.lower(root.get("notes")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
