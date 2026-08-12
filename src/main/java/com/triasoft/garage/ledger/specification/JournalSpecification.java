package com.triasoft.garage.ledger.specification;

import com.triasoft.garage.ledger.constants.JournalStatusEnum;
import com.triasoft.garage.ledger.entity.Journal;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JournalSpecification {

    public static Specification<Journal> build(String referenceType, JournalStatusEnum status,
                                               LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (referenceType != null && !referenceType.isBlank()) {
                predicates.add(cb.equal(root.get("referenceType"), referenceType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("journalDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("journalDate"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}
