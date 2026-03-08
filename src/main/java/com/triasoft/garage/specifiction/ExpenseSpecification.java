package com.triasoft.garage.specifiction;

import com.triasoft.garage.entity.ChartOfAccount;
import com.triasoft.garage.entity.Expense;
import com.triasoft.garage.model.common.FilterRq;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ExpenseSpecification {

    public static Specification<Expense> buildGeneralSearchQuery(FilterRq filter, String type) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if("P".equalsIgnoreCase(type)){
                predicates.add(cb.isNotNull(root.get("purchase")));
            }else{
                predicates.add(cb.isNull(root.get("purchase")));
            }
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), filter.getToDate()));
            }
            if (filter.getTypeId() != null) {
                predicates.add(cb.equal(root.get("expenseAccount").get("id"), filter.getTypeId()));
            }
            if (filter.getSearchText() != null && !filter.getSearchText().isBlank()) {
                String pattern = "%" + filter.getSearchText().toLowerCase() + "%";
                Join<Expense, ChartOfAccount> account = root.join("expenseAccount", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(account.get("name")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
