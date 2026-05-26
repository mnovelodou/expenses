package com.novelosoftware.expenses.mappers;

import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.SubCategory;
import com.novelosoftware.expenses.entities.ExpenseEntity;

/**
 * Entity <-> DTO expense mapper
 */
public final class ExpenseMapper {
    
    private ExpenseMapper() {}

    /**
     * Maps the given expense DTO into the internal expense entity
     * @param expense API/DTO Expense
     * @return corresponding internal ExpenseEntity
     */
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

    /**
     * Maps the given the internal expense entity into an expense DTO
     * @param entity Expense entity
     * @return corresponding expense DTO
     */
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
