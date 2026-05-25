package com.novelosoftware.expenses.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.novelosoftware.expenses.dto.CreateExpenseRequest;
import com.novelosoftware.expenses.dto.CreateExpenseResponse;
import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.SubCategory;
import com.novelosoftware.expenses.exceptions.GlobalExceptionHandler;
import com.novelosoftware.expenses.services.ExpenseService;

@WebMvcTest(ExpenseController.class)
@Import(GlobalExceptionHandler.class)
public class ExpenseControllerTest {
    
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    @Test
    void create_testHappyPath() throws Exception {
        when(expenseService.create(any())).thenReturn(new CreateExpenseResponse(anExpense(1L)));
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
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
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.expenseId").value(1))
            .andExpect(jsonPath("$.value.expenseDate").value("2026-05-25"))
            .andExpect(jsonPath("$.value.accountId").value(1))
            .andExpect(jsonPath("$.value.amount").value(1000.00))
            .andExpect(jsonPath("$.value.description").value("Expensive Tacos"));
        
        verify(expenseService).create(new CreateExpenseRequest(anExpense(null)));
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
