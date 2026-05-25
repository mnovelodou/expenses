package com.novelosoftware.expenses.mappers;

import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.SubCategory;
import com.novelosoftware.expenses.entities.ExpenseEntity;

public final class ExpenseMapper {
    
    private ExpenseMapper() {}

    public static ExpenseEntity toEntity(Expense expense) {
        return new ExpenseEntity(
            expense.expenseId(),
            expense.expenseDate(),
            expense.accountId(),
            expense.amount(),
            expense.description(),
            CategoryMapper.getCategory(expense.subCategory()).name(),
            expense.subCategory().name(),
            expense.createdBy()
        );
    }

    public static Expense toDto(ExpenseEntity entity) {
        return new Expense(
            entity.expenseId(),
            entity.expenseDate(),
            entity.accountId(),
            entity.amount(),
            entity.description(),
            SubCategory.valueOf(entity.subcategory()),
            entity.createdBy()
        );
    }
}
