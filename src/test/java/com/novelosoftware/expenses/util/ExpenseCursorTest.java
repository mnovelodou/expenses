package com.novelosoftware.expenses.util;

import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.InvalidCursorException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseCursorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 5, 15);
    private static final Long ID = 98L;

    @Test
    void encode_decode_roundTrip() {
        String token = ExpenseCursor.encode(DATE, ID);

        assertNotNull(token);
        assertFalse(token.isBlank());

        ExpenseCursor.DecodedCursor cursor = ExpenseCursor.decode(token);

        assertEquals(DATE, cursor.date());
        assertEquals(ID, cursor.id());
    }

    @Test
    void encode_producesUrlSafeToken() {
        String token = ExpenseCursor.encode(DATE, ID);
        // URL-safe base64 must not contain '+', '/', or '='
        assertFalse(token.contains("+"));
        assertFalse(token.contains("/"));
        assertFalse(token.contains("="));
    }

    @Test
    void decode_malformedToken_throwsInvalidCursorException() {
        assertThrows(InvalidCursorException.class, () -> ExpenseCursor.decode("not-valid-base64!!!"));
    }

    @Test
    void decode_validBase64ButInvalidJson_throwsInvalidCursorException() {
        String garbage = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("this is not json".getBytes());
        assertThrows(InvalidCursorException.class, () -> ExpenseCursor.decode(garbage));
    }

    @Test
    void decode_missingDateField_throwsInvalidCursorException() {
        String json = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"id\":98}".getBytes());
        assertThrows(InvalidCursorException.class, () -> ExpenseCursor.decode(json));
    }

    @Test
    void decode_missingIdField_throwsInvalidCursorException() {
        String json = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"d\":\"2026-05-15\"}".getBytes());
        assertThrows(InvalidCursorException.class, () -> ExpenseCursor.decode(json));
    }

    @Test
    void decode_nullIdField_throwsInvalidCursorException() {
        String json = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"d\":\"2026-05-15\",\"id\":null}".getBytes());
        assertThrows(InvalidCursorException.class, () -> ExpenseCursor.decode(json));
    }

    @Test
    void decode_zeroId_throwsInvalidCursorException() {
        String json = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"d\":\"2026-05-15\",\"id\":0}".getBytes());
        assertThrows(InvalidCursorException.class, () -> ExpenseCursor.decode(json));
    }

    @Test
    void decode_negativeId_throwsInvalidCursorException() {
        String json = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"d\":\"2026-05-15\",\"id\":-1}".getBytes());
        assertThrows(InvalidCursorException.class, () -> ExpenseCursor.decode(json));
    }
}
