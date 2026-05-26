package com.novelosoftware.expenses.mappers;

import com.novelosoftware.expenses.dto.Category;
import com.novelosoftware.expenses.dto.SubCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link CategoryMapperTest.class}
 */
class CategoryMapperTest {

    @ParameterizedTest(name = "{0} has a category mapping")
    @EnumSource(SubCategory.class)
    void allSubCategoriesHaveMapping(SubCategory subCategory) {
        var category = CategoryMapper.getCategory(subCategory);
        assertNotNull(category, "Missing mapping for SubCategory: " + subCategory);
    }

    @Test
    void spot_checks() {
        assertEquals(Category.CAR,                  CategoryMapper.getCategory(SubCategory.GAS));
        assertEquals(Category.CAR,                  CategoryMapper.getCategory(SubCategory.TOLLS));
        assertEquals(Category.CAR,                  CategoryMapper.getCategory(SubCategory.RIDESHARE));
        assertEquals(Category.SERVICES,             CategoryMapper.getCategory(SubCategory.INTERNET));
        assertEquals(Category.SERVICES,             CategoryMapper.getCategory(SubCategory.ELECTRICITY));
        assertEquals(Category.GENERAL,              CategoryMapper.getCategory(SubCategory.RESTAURANT));
        assertEquals(Category.DEBT,                 CategoryMapper.getCategory(SubCategory.CARD));
        assertEquals(Category.INCOME,               CategoryMapper.getCategory(SubCategory.INCOME));
        assertEquals(Category.INTERNATIONAL_TRANSFER, CategoryMapper.getCategory(SubCategory.INTERNATIONAL_TRANSFER));
        assertEquals(Category.CAR_MONTHLY_PAYMENT,  CategoryMapper.getCategory(SubCategory.CAR_MONTHLY_PAYMENT));
    }
}
