package com.novelosoftware.expenses.mappers;

import com.novelosoftware.expenses.dto.Category;
import com.novelosoftware.expenses.dto.SubCategory;

import java.util.Map;

/**
 * Provides a static mapping from SubCategory to its parent Category.
 * The map is immutable and built at class-load time.
 */
public final class CategoryMapper {

    private CategoryMapper() {}

    private static final Map<SubCategory, Category> SUBCATEGORY_TO_CATEGORY =
        Map.ofEntries(
            Map.entry(SubCategory.SAVINGS,                Category.MISCELLANEOUS),
            Map.entry(SubCategory.MISCELLANEOUS,          Category.MISCELLANEOUS),
            Map.entry(SubCategory.RENT,                   Category.RENT),
            Map.entry(SubCategory.GROCERIES,              Category.GROCERIES),
            Map.entry(SubCategory.CREDIT_CARD_PAYMENT,    Category.MISCELLANEOUS),
            Map.entry(SubCategory.SPOUSE,                 Category.SPOUSE),
            Map.entry(SubCategory.CAR_ACCESSORIES,        Category.CAR),
            Map.entry(SubCategory.VACATION,               Category.MISCELLANEOUS),
            Map.entry(SubCategory.TOLLS,                  Category.CAR),
            Map.entry(SubCategory.FOOD,                   Category.FOOD),
            Map.entry(SubCategory.UTILITIES,              Category.SERVICES),
            Map.entry(SubCategory.TRANSFER,               Category.MISCELLANEOUS),
            Map.entry(SubCategory.CARD,                   Category.DEBT),
            Map.entry(SubCategory.LEASING,                Category.CAR),
            Map.entry(SubCategory.GAS,                    Category.CAR),
            Map.entry(SubCategory.OUTING,                 Category.GENERAL),
            Map.entry(SubCategory.NANNY,                  Category.SERVICES),
            Map.entry(SubCategory.CLEANING,               Category.SERVICES),
            Map.entry(SubCategory.CURRENT_EXPENSE,        Category.GENERAL),
            Map.entry(SubCategory.TRUCK,                  Category.GENERAL),
            Map.entry(SubCategory.CELPHONE_BILL,          Category.SERVICES),
            Map.entry(SubCategory.RESTAURANT,             Category.GENERAL),
            Map.entry(SubCategory.ELECTRICITY,            Category.SERVICES),
            Map.entry(SubCategory.SERVICES,               Category.SERVICES),
            Map.entry(SubCategory.SOFTWARE,               Category.GENERAL),
            Map.entry(SubCategory.ENTERTAINMENT,          Category.GENERAL),
            Map.entry(SubCategory.CAR_MONTHLY_PAYMENT,    Category.CAR_MONTHLY_PAYMENT),
            Map.entry(SubCategory.SUBSCRIPTIONS,          Category.SERVICES),
            Map.entry(SubCategory.LOAN,                   Category.MISCELLANEOUS),
            Map.entry(SubCategory.OTHER,                  Category.GENERAL),
            Map.entry(SubCategory.INTERNATIONAL_TRANSFER, Category.INTERNATIONAL_TRANSFER),
            Map.entry(SubCategory.RIDESHARE,              Category.CAR),
            Map.entry(SubCategory.HOME,                   Category.MISCELLANEOUS),
            Map.entry(SubCategory.DEBT,                   Category.DEBT),
            Map.entry(SubCategory.PARKING,                Category.CAR),
            Map.entry(SubCategory.CAR_WASH,               Category.CAR),
            Map.entry(SubCategory.HEALTH,                 Category.GENERAL),
            Map.entry(SubCategory.CAR,                    Category.CAR),
            Map.entry(SubCategory.LOST,                   Category.GENERAL),
            Map.entry(SubCategory.SETTLEMENT,             Category.MISCELLANEOUS),
            Map.entry(SubCategory.INTERNET,               Category.SERVICES),
            Map.entry(SubCategory.TIP,                    Category.GENERAL),
            Map.entry(SubCategory.CLOTHING,               Category.GENERAL),
            Map.entry(SubCategory.GADGETS,                Category.MISCELLANEOUS),
            Map.entry(SubCategory.INCOME,                 Category.INCOME)
        );

    /**
     * Returns the parent Category for a given SubCategory.
     *
     * @param subCategory the subcategory to look up
     * @return the corresponding Category
     * @throws IllegalArgumentException if the subcategory has no mapping (should never happen
     *                                  if the map covers all enum values)
     */
    public static Category getCategory(SubCategory subCategory) {
        var category = SUBCATEGORY_TO_CATEGORY.get(subCategory);
        if (category == null) {
            throw new IllegalArgumentException("No category mapping for subcategory: " + subCategory);
        }
        return category;
    }
}
