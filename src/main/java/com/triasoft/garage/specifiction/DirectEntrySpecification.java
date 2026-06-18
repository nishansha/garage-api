package com.triasoft.garage.specifiction;

import com.triasoft.garage.constants.TransactionDirectionEnum;
import com.triasoft.garage.entity.DirectEntry;
import com.triasoft.garage.model.common.FilterRq;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DirectEntrySpecification {

    public static Specification<DirectEntry> buildSearchQuery(FilterRq filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("entryDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("entryDate"), filter.getToDate()));
            }
            if (filter.getTypeId() != null) {
                try {
                    predicates.add(cb.equal(root.get("chartOfAccount").get("id"), Long.parseLong(filter.getTypeId())));
                } catch (NumberFormatException ignored) {}
            }
            if (filter.getStaffId() != null && !filter.getStaffId().isBlank()) {
                try {
                    TransactionDirectionEnum direction = TransactionDirectionEnum.valueOf(filter.getStaffId().toUpperCase());
                    predicates.add(cb.equal(root.get("direction"), direction));
                } catch (IllegalArgumentException ignored) {}
            }
            if (filter.getSearchText() != null && !filter.getSearchText().isBlank()) {
                String pattern = "%" + filter.getSearchText().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("partyName")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("referenceNo")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
