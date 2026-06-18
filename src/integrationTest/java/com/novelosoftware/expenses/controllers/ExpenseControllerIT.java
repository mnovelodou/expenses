package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.BaseIT;
import com.novelosoftware.expenses.util.ExpenseCursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExpenseControllerIT extends BaseIT {

    private static final String USER = "user-expense-it";

    private long firstAccountId;
    private long secondAccountId;

    @BeforeEach
    void setUp() throws Exception {
        firstAccountId = createAccount("Test Account", USER);
        secondAccountId = createAccount("Second account", USER);
    }

    // -------------------------------------------------------------------------
    // GET /expenses
    // -------------------------------------------------------------------------

    @Test
    void list_happyPath_firstPage_returnsItemsNewestFirst() throws Exception {
        createExpenseOnDate(firstAccountId, USER, "2026-05-10");
        createExpenseOnDate(firstAccountId, USER, "2026-05-20");

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(2))
            // newest first
            .andExpect(jsonPath("$.content[0].expenseDate").value("2026-05-20"))
            .andExpect(jsonPath("$.content[1].expenseDate").value("2026-05-10"))
            .andExpect(jsonPath("$.content[0].createdBy").value(USER))
            .andExpect(jsonPath("$.pageSize").value(20));
    }

    @Test
    void list_defaultsToLastMonth_onlyLastMonthExpensesReturned() throws Exception {
        LocalDate today       = LocalDate.now();
        LocalDate yesterday   = today.minusDays(1);
        LocalDate twoDaysAgo  = today.minusDays(2);
        LocalDate twoMonthsAgo = YearMonth.now().minusMonths(2).atDay(15); // solidly 2 months back
        LocalDate inLastMonth  = YearMonth.now().minusMonths(1).atDay(15); // solidly in last month

        for (LocalDate date : List.of(today, yesterday, twoDaysAgo, twoMonthsAgo, inLastMonth)) {
            createExpenseOnDate(firstAccountId, USER, date.toString());
        }

        // Compute expected count dynamically so the test is robust even when
        // today-1 or today-2 happen to fall in the previous calendar month
        // (e.g. when today is the 1st or 2nd of the month).
        LocalDate windowStart = YearMonth.now().minusMonths(1).atDay(1);
        LocalDate windowEnd   = YearMonth.now().minusMonths(1).atEndOfMonth();
        int expectedCount = (int) Stream.of(today, yesterday, twoDaysAgo, twoMonthsAgo, inLastMonth)
            .filter(d -> !d.isBefore(windowStart) && !d.isAfter(windowEnd))
            .count();

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(expectedCount));
    }

    @Test
    void list_cursorPagination_secondPageFollowsFirst() throws Exception {
        // Create 3 expenses in range
        createExpenseOnDate(firstAccountId, USER, "2026-05-10");
        createExpenseOnDate(firstAccountId, USER, "2026-05-15");
        createExpenseOnDate(firstAccountId, USER, "2026-05-20");

        // First page: limit=2
        String firstPageResponse = mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.nextCursor").isString())
            .andReturn().getResponse().getContentAsString();

        String nextCursor = objectMapper.readTree(firstPageResponse).path("nextCursor").asText();

        // Second page using cursor
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("limit", "2")
                .param("cursor", nextCursor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void list_missingUserId_defaultsToCaller_returns200() throws Exception {
        // user_id is optional; omitting it scopes the query to the authenticated caller.
        createExpenseOnDate(firstAccountId, USER, "2026-05-10");

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].createdBy").value(USER));
    }

    @Test
    void list_requestedUserNotCaller_returns404() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", "someone-else")
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isNotFound());
    }

    @Test
    void list_malformedCursor_returns400() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("cursor", "not-a-valid-cursor!!"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void list_cursorDateOutsideRange_returns400() throws Exception {
        String cursor = ExpenseCursor.encode(LocalDate.of(2026, 3, 1), 10L);

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("cursor", cursor))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void list_rangeTooLarge_returns400() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-01-01")
                .param("end_date", "2026-06-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void list_endDateBeforeStartDate_returns400() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-31")
                .param("end_date", "2026-05-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void list_malformedStartDate_returns400() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "not-a-date")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void list_malformedLimit_returns400() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("limit", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void list_limitAboveCap_returns400() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("limit", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // -------------------------------------------------------------------------
    // GET /expenses — optional filters
    // -------------------------------------------------------------------------

    @Test
    void list_filterByCategory_returnsOnlyMatchingExpenses() throws Exception {
        // RESTAURANT → category GENERAL; GROCERIES → category GROCERIES
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-10", "RESTAURANT");
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-15", "GROCERIES");

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("category", "GROCERIES"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].subCategory").value("GROCERIES"));
    }

    @Test
    void list_filterBySubcategory_returnsOnlyMatchingExpenses() throws Exception {
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-10", "RESTAURANT");
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-15", "GROCERIES");

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("subcategory", "GROCERIES"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].subCategory").value("GROCERIES"));
    }

    @Test
    void list_filterByAccountId_returnsOnlyMatchingExpenses() throws Exception {
        createExpenseOnDate(firstAccountId, USER, "2026-05-10");
        createExpenseOnDate(secondAccountId, USER, "2026-05-15");

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("account_id", String.valueOf(firstAccountId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].accountId").value(firstAccountId));
    }

    @Test
    void list_filterBySubcategoryAndAccountId_returnsOnlyMatchingExpenses() throws Exception {
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-10", "GROCERIES");
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-12", "RESTAURANT");
        createExpenseWithSubcategoryOnDate(secondAccountId, USER, "2026-05-14", "GROCERIES");

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("subcategory", "GROCERIES")
                .param("account_id", String.valueOf(firstAccountId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].subCategory").value("GROCERIES"))
            .andExpect(jsonPath("$.content[0].accountId").value(firstAccountId));
    }

    @Test
    void list_categoryAndSubcategoryBothProvided_returns400() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("category", "GENERAL")
                .param("subcategory", "RESTAURANT"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void list_filterByCategory_withCursorPagination_works() throws Exception {
        // Create 3 GROCERIES expenses in range
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-10", "GROCERIES");
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-15", "GROCERIES");
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-20", "GROCERIES");
        // Create a non-matching expense that should not appear
        createExpenseWithSubcategoryOnDate(firstAccountId, USER, "2026-05-18", "RESTAURANT");

        String firstPageResponse = mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("category", "GROCERIES")
                .param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.nextCursor").isString())
            .andReturn().getResponse().getContentAsString();

        String nextCursor = objectMapper.readTree(firstPageResponse).path("nextCursor").asText();

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("category", "GROCERIES")
                .param("limit", "2")
                .param("cursor", nextCursor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void list_resultsAreInDescendingOrder() throws Exception {
        createExpenseOnDate(firstAccountId, USER, "2026-05-10");
        createExpenseOnDate(firstAccountId, USER, "2026-05-20");
        createExpenseOnDate(firstAccountId, USER, "2026-05-15");

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].expenseDate").value("2026-05-20"))
            .andExpect(jsonPath("$.content[1].expenseDate").value("2026-05-15"))
            .andExpect(jsonPath("$.content[2].expenseDate").value("2026-05-10"));
    }

    // -------------------------------------------------------------------------
    // POST /expenses
    // -------------------------------------------------------------------------

    @Test
    void create_happyPath_returns201WithWrappedExpense() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.expenseId").isNumber())
            .andExpect(jsonPath("$.value.expenseDate").value("2026-05-27"))
            .andExpect(jsonPath("$.value.accountId").value(firstAccountId))
            .andExpect(jsonPath("$.value.amount").value(42.50))
            .andExpect(jsonPath("$.value.description").value("Test tacos"))
            .andExpect(jsonPath("$.value.subCategory").value("RESTAURANT"))
            .andExpect(jsonPath("$.value.createdBy").value("user-expense-it"));
    }

    @Test
    void create_accountDoesNotExist_returns404() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": 999999,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_accountBelongsToDifferentUser_returns403() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-B" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void create_missingExpenseDate_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": null, "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("expenseDate cannot be null"));
    }

    @Test
    void create_missingAccountId_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": null,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingAmount_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": null, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingDescription_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingSubCategory_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": null, "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingCreatedBy_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_invalidSubCategoryEnum_returns400WithAcceptedValues() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "INVALID_ENUM", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("INVALID_ENUM")));
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ unclosed"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    // -------------------------------------------------------------------------
    // Multi-step flow: verify CategoryMapper → DB → ExpenseMapper round-trip
    // -------------------------------------------------------------------------

    @Test
    void create_subCategoryDerivedAndRoundTripped() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 15.00, "description": "Lunch",
                      "subCategory": "GROCERIES", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.subCategory").value("GROCERIES"));
    }

    @Test
    void update_happyPath() throws Exception {
        Long expenseId = createExpense(firstAccountId, USER);
        mockMvc.perform(put("/expenses/" + expenseId)
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": %d,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(expenseId, secondAccountId, USER)))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.value.accountId").value(secondAccountId));
    }

    @Test
    void update_unauthorizedOwnerChange() throws Exception {
        Long expenseId = createExpense(firstAccountId, USER);
        mockMvc.perform(put("/expenses/" + expenseId)
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": %d,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(expenseId, firstAccountId, "another-user")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("Cannot write expenses on behalf of another user"));
    }

    @Test
    void update_referencingAnotherUsersAccount_returns404() throws Exception {
        // Pointing at an account owned by someone else must be hidden as 404 (no existence
        // disclosure), not 403 — same non-disclosure guarantee as any non-owned resource.
        Long expenseId = createExpense(firstAccountId, USER);
        Long thirdAccount = createAccount("third-person-account", "another-user-id");
        mockMvc.perform(put("/expenses/" + expenseId)
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": %d,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(expenseId, thirdAccount, USER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void update_expenseDoesNotExists() throws Exception {
        mockMvc.perform(put("/expenses/999999")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": 999999,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(secondAccountId, USER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Expense id 999999 not found"));
    }

    // -------------------------------------------------------------------------
    // GET /expenses/{id}
    // -------------------------------------------------------------------------

    @Test
    void getById_existingExpense_returns200WithBody() throws Exception {
        long expenseId = createExpense(firstAccountId, USER);
        mockMvc.perform(get("/expenses/" + expenseId)
                .with(fullScopeJwtAs(USER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expenseId").value(expenseId))
            .andExpect(jsonPath("$.accountId").value(firstAccountId))
            .andExpect(jsonPath("$.createdBy").value(USER));
    }

    @Test
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/expenses/999999")
                .with(fullScopeJwtAs(USER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // DELETE /expenses/{id}
    // -------------------------------------------------------------------------

    @Test
    void delete_existingExpense_returns204AndIsGone() throws Exception {
        long expenseId = createExpense(firstAccountId, USER);
        mockMvc.perform(delete("/expenses/" + expenseId)
                .with(fullScopeJwtAs(USER)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/expenses/" + expenseId)
                .with(fullScopeJwtAs(USER)))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_unknownId_returns404() throws Exception {
        mockMvc.perform(delete("/expenses/999999")
                .with(fullScopeJwtAs(USER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // POST /expenses/bulk
    // -------------------------------------------------------------------------

    @Test
    void bulkCreate_happyPath_returns201AndAllExpensesInDB() throws Exception {
        String payload = """
            { "expenses": [
                { "value": { "expenseDate": "2026-05-10", "accountId": %d, "amount": 10.00,
                  "description": "Lunch", "subCategory": "RESTAURANT", "createdBy": "%s" } },
                { "value": { "expenseDate": "2026-05-11", "accountId": %d, "amount": 20.00,
                  "description": "Groceries", "subCategory": "GROCERIES", "createdBy": "%s" } }
            ] }
            """.formatted(firstAccountId, USER, firstAccountId, USER);

        mockMvc.perform(post("/expenses/bulk")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expenses.length()").value(2))
            .andExpect(jsonPath("$.expenses[0].value.expenseId").isNumber())
            .andExpect(jsonPath("$.expenses[0].value.description").value("Lunch"))
            .andExpect(jsonPath("$.expenses[1].value.description").value("Groceries"));

        // Verify both rows are visible via GET
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void bulkCreate_oneItemInvalidAccount_returns400AndNoExpensesCreated() throws Exception {
        String payload = """
            { "expenses": [
                { "value": { "expenseDate": "2026-05-10", "accountId": %d, "amount": 10.00,
                  "description": "Valid", "subCategory": "RESTAURANT", "createdBy": "%s" } },
                { "value": { "expenseDate": "2026-05-11", "accountId": 999999, "amount": 20.00,
                  "description": "Bad account", "subCategory": "GROCERIES", "createdBy": "%s" } }
            ] }
            """.formatted(firstAccountId, USER, USER);

        mockMvc.perform(post("/expenses/bulk")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNotFound());

        // No expenses should have been persisted
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void bulkCreate_emptyList_returns400() throws Exception {
        mockMvc.perform(post("/expenses/bulk")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"expenses\": [] }"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void bulkCreate_exceedsBatchSizeLimit_returns400() throws Exception {
        // Build a list of 201 expenses
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 201; i++) {
            if (i > 0) items.append(",");
            items.append("""
                { "value": { "expenseDate": "2026-05-10", "accountId": %d, "amount": 5.00,
                  "description": "Item %d", "subCategory": "RESTAURANT", "createdBy": "%s" } }
                """.formatted(firstAccountId, i, USER));
        }
        String payload = "{ \"expenses\": [" + items + "] }";

        mockMvc.perform(post("/expenses/bulk")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());
    }

    @Test
    void update_accountNotFound() throws Exception {
        long expenseId = createExpense(firstAccountId, USER);
        mockMvc.perform(put("/expenses/" + expenseId)
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": %d,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(expenseId, 999999, USER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Account with ID 999999 not found."));
    }

    // -------------------------------------------------------------------------
    // transactionAmount: split-and-search, creation default, partial update
    // -------------------------------------------------------------------------

    @Test
    void create_withoutTransactionAmount_defaultsToAmount() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Solo expense",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.amount").value(42.50))
            .andExpect(jsonPath("$.value.transactionAmount").value(42.50));
    }

    @Test
    void splitTransaction_searchByTransactionAmount_returnsOnlyMatchingLines() throws Exception {
        // One $100 transaction split into two lines, both carrying transactionAmount = 100.00.
        createExpenseWithTransactionAmount(firstAccountId, USER, "2026-05-10", "60.00", "100.00");
        createExpenseWithTransactionAmount(firstAccountId, USER, "2026-05-12", "40.00", "100.00");
        // An unrelated expense with a different transaction amount must be excluded.
        createExpenseWithTransactionAmount(firstAccountId, USER, "2026-05-15", "25.00", "25.00");

        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("transaction_amount", "100.00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].expenseDate").value("2026-05-12"))
            .andExpect(jsonPath("$.content[0].amount").value(40.00))
            .andExpect(jsonPath("$.content[0].transactionAmount").value(100.00))
            .andExpect(jsonPath("$.content[1].expenseDate").value("2026-05-10"))
            .andExpect(jsonPath("$.content[1].amount").value(60.00))
            .andExpect(jsonPath("$.content[1].transactionAmount").value(100.00));
    }

    @Test
    void splitTransaction_searchByTransactionAmount_cursorPaginationIntact() throws Exception {
        createExpenseWithTransactionAmount(firstAccountId, USER, "2026-05-10", "60.00", "100.00");
        createExpenseWithTransactionAmount(firstAccountId, USER, "2026-05-12", "40.00", "100.00");

        // First page with limit 1: newest matching line, plus a nextCursor.
        String firstPage = mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("transaction_amount", "100.00")
                .param("limit", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].expenseDate").value("2026-05-12"))
            .andExpect(jsonPath("$.nextCursor").isNotEmpty())
            .andReturn().getResponse().getContentAsString();

        String cursor = objectMapper.readTree(firstPage).path("nextCursor").asText();

        // Second page: the older matching line, with the filter resupplied alongside the cursor.
        mockMvc.perform(get("/expenses")
                .with(fullScopeJwtAs(USER))
                .param("user_id", USER)
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("transaction_amount", "100.00")
                .param("limit", "1")
                .param("cursor", cursor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].expenseDate").value("2026-05-10"))
            .andExpect(jsonPath("$.content[0].transactionAmount").value(100.00));
    }

    @Test
    void update_omittingTransactionAmount_preservesStoredValue() throws Exception {
        long expenseId = createExpenseWithTransactionAmount(firstAccountId, USER, "2026-05-10", "60.00", "100.00");

        // Change the amount but omit transactionAmount: stored 100.00 must be preserved (not re-defaulted to 70.00).
        mockMvc.perform(put("/expenses/" + expenseId)
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseId": %d, "expenseDate": "2026-05-10", "accountId": %d,
                      "amount": 70.00, "description": "Test expense",
                      "subCategory": "RESTAURANT", "createdBy": "%s" } }
                """.formatted(expenseId, firstAccountId, USER)))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.value.amount").value(70.00))
            .andExpect(jsonPath("$.value.transactionAmount").value(100.00));
    }

    @Test
    void create_transactionAmountTooPrecise_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "transactionAmount": 100.123, "description": "Too precise",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest());
    }

    private long createExpenseWithTransactionAmount(long accountId, String userId, String date,
                                                    String amount, String transactionAmount) throws Exception {
        String response = mockMvc.perform(post("/expenses")
                .with(fullScopeJwtAs(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "%s", "accountId": %d, "amount": %s,
                      "transactionAmount": %s, "description": "Split line",
                      "subCategory": "RESTAURANT", "createdBy": "%s" } }
                    """.formatted(date, accountId, amount, transactionAmount, userId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("value").path("expenseId").asLong();
    }
}
