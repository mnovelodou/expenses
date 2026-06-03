package com.novelosoftware.expenses.dto;

/**
 * A union type representing a mutually exclusive filter on either a category name
 * or a subcategory name. The sealed hierarchy ensures only one of the two variants
 * can be constructed, making the mutual-exclusivity constraint explicit in the type
 * system rather than a runtime check.
 *
 * <p>Designed for extensibility: user-defined categorisation schemes can be added
 * as new permitted subclasses without changing existing code that pattern-matches
 * on the existing variants.
 *
 * <p>Usage:
 * <pre>{@code
 * CategoryFilter f = CategoryFilter.ofCategory("GENERAL");
 * if (f.isCategory()) { ... }
 * }</pre>
 */
public sealed interface CategoryFilter
        permits CategoryFilter.ByCategoryName, CategoryFilter.BySubcategoryName {

    /** Returns true when this filter targets a top-level category name. */
    default boolean isCategory() { return this instanceof ByCategoryName; }

    /** Returns true when this filter targets a subcategory name. */
    default boolean isSubcategory() { return this instanceof BySubcategoryName; }

    /**
     * Creates a filter that matches on the given top-level category name.
     *
     * @param category the category name to match (case-sensitive)
     * @return a CategoryFilter scoped to the given category
     */
    static CategoryFilter ofCategory(String category) {
        return new ByCategoryName(category);
    }

    /**
     * Creates a filter that matches on the given subcategory name.
     *
     * @param subcategory the subcategory name to match (case-sensitive)
     * @return a CategoryFilter scoped to the given subcategory
     */
    static CategoryFilter ofSubcategory(String subcategory) {
        return new BySubcategoryName(subcategory);
    }

    /** Filter variant that matches expenses by top-level category name. */
    record ByCategoryName(String category) implements CategoryFilter {}

    /** Filter variant that matches expenses by subcategory name. */
    record BySubcategoryName(String subcategory) implements CategoryFilter {}
}
