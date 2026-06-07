package com.novelosoftware.expenses.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.novelosoftware.expenses.dto.Account;
import com.novelosoftware.expenses.dto.BulkCreateExpensesRequest;
import com.novelosoftware.expenses.dto.CreateExpenseRequest;
import com.novelosoftware.expenses.dto.CreateExpenseResponse;
import com.novelosoftware.expenses.dto.CursorPageResponse;
import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.UpdateExpenseRequest;
import com.novelosoftware.expenses.dto.UpdateExpenseResponse;
import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.mappers.ExpenseMapper;
import com.novelosoftware.expenses.repositories.ExpenseRepository;
import com.novelosoftware.expenses.util.DateWindowResolver;
import com.novelosoftware.expenses.util.DateWindow;
import com.novelosoftware.expenses.util.ExpenseCursor;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.*;

/**
 * ExpenseService contains business logic related to expenses.
 */
@Service
public class ExpenseService {
    
    private final ExpenseRepository repo;
    private final AccountService accountService;
    private final Clock clock;

    @Autowired
    public ExpenseService(ExpenseRepository repo, AccountService accountService) {
        this(repo, accountService, Clock.systemDefaultZone());
    }

    /** Package-private constructor for unit tests that need a controllable clock. */
    ExpenseService(ExpenseRepository repo, AccountService accountService, Clock clock) {
        this.repo = repo;
        this.accountService = accountService;
        this.clock = clock;
    }

    /**
     * Creates multiple expenses atomically.
     *
     * @param requests list of CreateExpenseRequest payloads (1–200 items)
     * @return the created expenses wrapped in CreateExpenseResponse objects
     */
    @Transactional
    public List<CreateExpenseResponse> bulkCreate(BulkCreateExpensesRequest request) {
        if (request == null || request.expenses() == null || request.expenses().isEmpty()) {
            throw createValidationException("expenses list must not be empty");
        }
        if (request.expenses().size() > 200) {
            throw createValidationException("expenses list must not exceed 200 items");
        }

        List<ExpenseEntity> entities = request.expenses().stream()
            .map(req -> {
                if (req == null || req.value() == null) {
                    throw createValidationException("Expense payload not provided");
                }
                expenseWriteValidations(req.value());
                return ExpenseMapper.toEntity(req.value());
            })
            .toList();

        return repo.bulkInsert(entities).stream()
            .map(e -> new CreateExpenseResponse(ExpenseMapper.toDto(e)))
            .toList();
    }

    /**
     * Creates an expense
     * @param request CreateExpenseRequest payload
     * @return the created Expense wrapped into a CreateExpenseResponse.
     */
    public CreateExpenseResponse create(CreateExpenseRequest request) {
        if (request == null || request.value() == null) {
            throw createValidationException("Expense payload not provided");
        }

        Expense expense = request.value();
        expenseWriteValidations(expense);

        ExpenseEntity expenseEntity = ExpenseMapper.toEntity(expense);
        ExpenseEntity createdEntity = repo.create(expenseEntity);
        return new CreateExpenseResponse(ExpenseMapper.toDto(createdEntity));
    }

    public Expense getById(Long id) {
        ExpenseEntity entity = repo.get(id).orElseThrow(() -> createExpenseNotFoundException(id));
        return ExpenseMapper.toDto(entity);
    }

    public void delete(Long id) {
        if (!repo.delete(id)) {
            throw createExpenseNotFoundException(id);
        }
    }

    public UpdateExpenseResponse update(Long id, UpdateExpenseRequest request) {
        if (request == null || request.value() == null)  {
            throw createValidationException("Expense payload not provided");
        }

        if (id == null) {
            throw createValidationException("ID not provided");
        }

        Expense expense = request.value();
        expenseWriteValidations(expense);

        // Payload is validated now we can bring previous version and make sure this is the owner.
        ExpenseEntity oldExpense = repo.get(id).orElseThrow(() -> createExpenseNotFoundException(id));

        if (!expense.createdBy().equals(oldExpense.createdBy())) {
            throw createUnauthorizedExpenseException("Expense not owned by viewer");
        }

        ExpenseEntity newExpense = ExpenseMapper.toEntity(expense);
        ExpenseEntity updatedEntity = repo.update(id, newExpense).orElseThrow(() -> createExpenseNotFoundException(id));
        return new UpdateExpenseResponse(ExpenseMapper.toDto(updatedEntity));
    }

    /**
     * Returns a cursor-paginated list of expenses for the given user within the specified date window,
     * with optional filters on category, subcategory, and accountId.
     *
     * <p>Date defaults (each param is resolved independently):
     * <ul>
     *   <li>Both absent → previous calendar month</li>
     *   <li>Only {@code startDate} → {@code endDate = startDate + 1 month}</li>
     *   <li>Only {@code endDate}   → {@code startDate = endDate - 1 month}</li>
     * </ul>
     *
     * <p>{@code category} and {@code subcategory} are mutually exclusive — supplying both
     * results in a validation error. {@code accountId} may be combined with either.
     *
     * @param userId      required; the user whose expenses to list
     * @param startDate   optional start of the date window
     * @param endDate     optional end of the date window
     * @param limit       optional page size; defaults to 20, max 100
     * @param cursorToken optional opaque cursor from a previous response
     * @param category    optional category filter; mutually exclusive with subcategory
     * @param subcategory optional subcategory filter; mutually exclusive with category
     * @param accountId   optional account filter
     * @return a page of expenses with an optional next-page cursor
     */
    public CursorPageResponse<Expense> listByUser(String userId,
                                                   LocalDate startDate,
                                                   LocalDate endDate,
                                                   Integer limit,
                                                   String cursorToken,
                                                   String category,
                                                   String subcategory,
                                                   Long accountId) {
        if (userId == null || userId.isBlank()) {
            throw createValidationException("user_id is required");
        }

        // --- Validate mutually exclusive filters ---
        if (category != null && subcategory != null) {
            throw createValidationException("category and subcategory are mutually exclusive; provide at most one");
        }

        // --- Resolve and validate date window ---
        DateWindow window = DateWindowResolver.resolve(startDate, endDate, clock);

        // --- Resolve limit ---
        int resolvedLimit = (limit == null) ? 20 : limit;
        if (resolvedLimit <= 0) {
            throw createValidationException("limit must be greater than 0");
        }
        if (resolvedLimit > 100) {
            throw createValidationException("limit must not exceed 100");
        }

        // --- Decode cursor ---
        ExpenseCursor.DecodedCursor cursor = null;
        if (cursorToken != null && !cursorToken.isBlank()) {
            cursor = ExpenseCursor.decode(cursorToken); // throws InvalidCursorException if malformed
            if (cursor.date().isBefore(window.startDate()) || cursor.date().isAfter(window.endDate())) {
                throw createInvalidCursorException("Cursor date is outside the requested date range");
            }
        }

        // --- Fetch and map ---
        List<ExpenseEntity> entities = repo.findByFiltersCursor(
            userId, window.startDate(), window.endDate(),
            category, subcategory, accountId,
            resolvedLimit, cursor);
        List<Expense> expenses = entities.stream().map(ExpenseMapper::toDto).toList();

        // --- Build next cursor ---
        String nextCursor = null;
        if (expenses.size() == resolvedLimit) {
            Expense last = expenses.get(expenses.size() - 1);
            nextCursor = ExpenseCursor.encode(last.expenseDate(), last.expenseId());
        }

        return new CursorPageResponse<>(expenses, nextCursor, resolvedLimit);
    }

    private void expenseWriteValidations(Expense expense) {
        if (expense.expenseDate() == null) {
            throw createValidationException("expenseDate cannot be null");
        }

        if (expense.accountId() == null) {
            throw createValidationException("accountId cannot be null");
        }

        if (expense.amount() == null) {
            throw createValidationException("amount cannot be null");
        }

        if (expense.description() == null || expense.description().isEmpty()) {
            throw createValidationException("description cannot be null");
        }

        if (expense.subCategory() == null) {
            throw createValidationException("subcategory cannot be null");
        }

        if (expense.createdBy() == null || expense.createdBy().isEmpty()) {
            throw createValidationException("createdBy cannot be null");
        }

        Account account = accountService.getById(expense.accountId());
        
        if (!account.createdBy().equals(expense.createdBy())) {
            throw createUnauthorizedExpenseException("User does not own the given account");
        }
    }
}
