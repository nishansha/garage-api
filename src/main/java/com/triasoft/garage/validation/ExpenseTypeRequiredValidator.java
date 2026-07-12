package com.triasoft.garage.validation;

import com.triasoft.garage.model.expense.ExpenseRq;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExpenseTypeRequiredValidator implements ConstraintValidator<ExpenseTypeRequired, ExpenseRq> {

    @Override
    public boolean isValid(ExpenseRq rq, ConstraintValidatorContext context) {
        if (rq == null) {
            return true;
        }
        boolean hasType = rq.getTypeId() != null;
        boolean hasTitle = rq.getTitle() != null && !rq.getTitle().trim().isEmpty();
        if (hasType || hasTitle) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("REQUIRED")
                .addPropertyNode("title")
                .addConstraintViolation();
        return false;
    }
}
