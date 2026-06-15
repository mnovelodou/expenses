package com.novelosoftware.expenses;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Arrays;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public abstract class BaseIT {

    // Singleton pattern: container starts once for the JVM, never stops between test classes.
    // @Testcontainers + @Container on an abstract class stops/restarts the container between
    // subclasses, which breaks the shared Spring ApplicationContext's cached datasource URL.
    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("expenses_test")
            .withUsername("test")
            .withPassword("test");
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /** period_start assigned to every account created via {@link #createAccount}; assert against this constant. */
    protected static final String DEFAULT_PERIOD_START = "2026-06-01";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected RequestPostProcessor jwt(String... scopes) {
        GrantedAuthority[] authorities = Arrays.stream(scopes)
                .map(s -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + s))
                .toArray(GrantedAuthority[]::new);
        return SecurityMockMvcRequestPostProcessors.jwt().authorities(authorities);
    }

    protected RequestPostProcessor fullScopeJwt() {
        return jwt("read:expenses", "write:expenses", "read:accounts", "write:accounts");
    }

    protected long createAccount(String name, String userId) throws Exception {
        String response = mockMvc.perform(post("/accounts")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "%s", "accountType": "DEBIT", "currency": "USD",
                      "initialAmount": 1000.00, "createdBy": "%s", "periodStart": "%s" } }
                    """.formatted(name, userId, DEFAULT_PERIOD_START)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("value").path("accountId").asLong();
    }

    protected long createExpense(long accountId, String userId) throws Exception {
        return createExpenseOnDate(accountId, userId, "2026-01-15");
    }

    protected long createExpenseOnDate(long accountId, String userId, String date) throws Exception {
        return createExpenseWithSubcategoryOnDate(accountId, userId, date, "RESTAURANT");
    }

    protected long createExpenseWithSubcategoryOnDate(long accountId, String userId, String date,
                                                       String subCategory) throws Exception {
        String response = mockMvc.perform(post("/expenses")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "%s", "accountId": %d, "amount": 42.50,
                      "description": "Test expense", "subCategory": "%s", "createdBy": "%s" } }
                    """.formatted(date, accountId, subCategory, userId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("value").path("expenseId").asLong();
    }
}
