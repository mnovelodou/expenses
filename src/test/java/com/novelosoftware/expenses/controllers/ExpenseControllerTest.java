package com.novelosoftware.expenses.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import com.novelosoftware.expenses.dto.CreateExpenseRequest;
import com.novelosoftware.expenses.dto.CreateExpenseResponse;
import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.SubCategory;
import com.novelosoftware.expenses.dto.UpdateExpenseRequest;
import com.novelosoftware.expenses.dto.UpdateExpenseResponse;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions;
import com.novelosoftware.expenses.exceptions.GlobalExceptionHandler;
import com.novelosoftware.expenses.services.ExpenseService;

/** 
 * Test class for {@link ExpenseController.class}
 */
@WebMvcTest(ExpenseController.class)
@Import(GlobalExceptionHandler.class)
public class ExpenseControllerTest {
    
    private static final String CREATE_PAYLOAD = """
                    {
                        "value": {
                            "expenseDate": "2026-05-25",
                            "accountId": 1,
                            "amount": 1000.00,
                            "description": "Expensive Tacos",
                            "subCategory": "RESTAURANT",
                            "createdBy": "user-1"
                        }
                    }
                """;

    private static final String UPDATE_PAYLOAD = """
                    {
                        "value": {
                            "expenseId": 1,
                            "expenseDate": "2026-05-25",
                            "accountId": 1,
                            "amount": 1000.00,
                            "description": "Expensive Tacos",
                            "subCategory": "RESTAURANT",
                            "createdBy": "user-1"
                        }
                    }
                """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    // -------------------------------------------------------------------------
    // GET /expenses/{id}
    // -------------------------------------------------------------------------

    @Test
    void getById_found_returns200() throws Exception {
        when(expenseService.getById(1L)).thenReturn(anExpense(1L));
        mockMvc.perform(get("/expenses/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expenseId").value(1))
            .andExpect(jsonPath("$.description").value("Expensive Tacos"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(expenseService.getById(999L))
            .thenThrow(ExpenseServiceExceptions.createExpenseNotFoundException(999L));
        mockMvc.perform(get("/expenses/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // DELETE /expenses/{id}
    // -------------------------------------------------------------------------

    @Test
    void delete_success_returns204() throws Exception {
        doNothing().when(expenseService).delete(1L);
        mockMvc.perform(delete("/expenses/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(ExpenseServiceExceptions.createExpenseNotFoundException(999L))
            .when(expenseService).delete(999L);
        mockMvc.perform(delete("/expenses/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_testHappyPath() throws Exception {
        when(expenseService.create(any())).thenReturn(new CreateExpenseResponse(anExpense(1L)));
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_PAYLOAD))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.expenseId").value(1))
            .andExpect(jsonPath("$.value.expenseDate").value("2026-05-25"))
            .andExpect(jsonPath("$.value.accountId").value(1))
            .andExpect(jsonPath("$.value.amount").value(1000.00))
            .andExpect(jsonPath("$.value.description").value("Expensive Tacos"));
        
        verify(expenseService).create(new CreateExpenseRequest(anExpense(null)));
    }

    @Test
    void create_testMalformedJSON() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "values": {
                            "expenseDate": "2026-05-25",
                            "accountId": 1,
                            "amount": 1000.00,
                            "description": "Expensive Tacos",
                            "subCategory": "RESTAURANT",
                            "createdBy": "user-1"
                        }
                """))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(expenseService);
    }

    @Test
    void update_testHappyPath() throws Exception {
        when(expenseService.update(anyLong(), any(UpdateExpenseRequest.class)))
            .thenReturn(new UpdateExpenseResponse(anExpense(1L)));
        mockMvc.perform(put("/expenses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(UPDATE_PAYLOAD))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.value.expenseId").value(1))
            .andExpect(jsonPath("$.value.expenseDate").value("2026-05-25"))
            .andExpect(jsonPath("$.value.accountId").value(1))
            .andExpect(jsonPath("$.value.amount").value(1000.00))
            .andExpect(jsonPath("$.value.description").value("Expensive Tacos"));

        verify(expenseService).update(1L, new UpdateExpenseRequest(anExpense(1L)));
    }

    @Test
    void update_testMalformedJSON() throws Exception {
        mockMvc.perform(put("/expenses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "values": {
                            "expenseId": 1
                            "expenseDate": "2026-05-25",
                            "accountId": 1,
                            "amount": 1000.00,
                            "description": "Expensive Tacos",
                            "subCategory": "RESTAURANT",
                            "createdBy": "user-1"
                        }
                """))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(expenseService);
    }

    @ParameterizedTest(name = "create_invalidExpense-{0}")
    @MethodSource("serviceExcptions")
    void create_invalidExpense(String testName, RuntimeException exception, ResultMatcher statusMatcher) throws Exception {
        when(expenseService.create(any()))
                .thenThrow(exception);
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_PAYLOAD))
            .andExpect(statusMatcher);
    }

    @ParameterizedTest(name = "update_invalidExpense-{0}")
    @MethodSource("serviceExcptions")
    void update_invalidExpense(String testName, RuntimeException exception, ResultMatcher statusMatcher) throws Exception {
        when(expenseService.update(anyLong(), any(UpdateExpenseRequest.class)))
                .thenThrow(exception);
        mockMvc.perform(put("/expenses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(UPDATE_PAYLOAD))
            .andExpect(statusMatcher);
    }

    static Stream<Arguments> serviceExcptions() {
        return Stream.of(
            Arguments.of(
                "unauthorized", 
                ExpenseServiceExceptions.createUnauthorizedExpenseException("forbidden"), 
                status().isForbidden()),
            Arguments.of(
                "bad_request", 
                ExpenseServiceExceptions.createValidationException("bad_request"), 
                status().isBadRequest()),
            Arguments.of(
                "internal", 
                new RuntimeException("internal"), 
                status().isInternalServerError()));
    }


    // -------------------------------------------------------------------------
    // GET /expenses — filter params (parameterized)
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "list_filter_{0}_delegatesToService")
    @MethodSource("filterCombinations")
    void list_filter_delegatesToService(
            String testName,
            String[] queryParams,
            String expectedCategory,
            String expectedSubcategory,
            Long expectedAccountId) throws Exception {

        when(expenseService.listByUser(any(), any(), any(), any(), any(),
            expectedCategory == null ? isNull() : eq(expectedCategory),
            expectedSubcategory == null ? isNull() : eq(expectedSubcategory),
            expectedAccountId == null ? isNull() : eq(expectedAccountId)))
            .thenReturn(new com.novelosoftware.expenses.dto.CursorPageResponse<>(List.of(), null, 20));

        var request = get("/expenses").param("user_id", "user-1");
        for (int i = 0; i < queryParams.length; i += 2) {
            request = request.param(queryParams[i], queryParams[i + 1]);
        }

        mockMvc.perform(request).andExpect(status().isOk());

        verify(expenseService).listByUser(eq("user-1"), isNull(), isNull(), isNull(), isNull(),
            expectedCategory == null ? isNull() : eq(expectedCategory),
            expectedSubcategory == null ? isNull() : eq(expectedSubcategory),
            expectedAccountId == null ? isNull() : eq(expectedAccountId));
    }

    static Stream<Arguments> filterCombinations() {
        return Stream.of(
            Arguments.of("no_filters",      new String[]{},                                     null,        null,         null),
            Arguments.of("category",         new String[]{"category", "Food"},                   "Food",      null,         null),
            Arguments.of("subcategory",      new String[]{"subcategory", "Groceries"},            null,        "Groceries",  null),
            Arguments.of("account_id",       new String[]{"account_id", "3"},                    null,        null,         3L),
            Arguments.of("cat_and_acct",     new String[]{"category", "Food", "account_id", "3"}, "Food",     null,         3L),
            Arguments.of("sub_and_acct",     new String[]{"subcategory", "Groceries", "account_id", "3"}, null, "Groceries", 3L)
        );
    }

    @Test
    void list_categoryAndSubcategoryBothProvided_returns400() throws Exception {
        when(expenseService.listByUser(any(), any(), any(), any(), any(),
            eq("Food"), eq("Groceries"), isNull()))
            .thenThrow(com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions
                .createValidationException("category and subcategory are mutually exclusive; provide at most one"));

        mockMvc.perform(get("/expenses")
                .param("user_id", "user-1")
                .param("category", "Food")
                .param("subcategory", "Groceries"))
            .andExpect(status().isBadRequest());
    }

    private Expense anExpense(Long id) {
        return new Expense(
            id,
            LocalDate.of(2026, 5, 25), 
            1L, 
            new BigDecimal("1000.00"),
            "Expensive Tacos",
            SubCategory.RESTAURANT,
            "user-1");
    }
}
