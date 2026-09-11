package com.example.erp.common.support;

/**
 * Pre-flight checks for the rules the schema does enforce but cannot report well.
 * <p>
 * Letting a unique index fail the insert gives the caller a constraint violation naming a
 * database object; checking first gives them a 400 whose message starts with the field name,
 * which is what the client needs to highlight the offending input.
 */
public final class Guards {

    private Guards() {
    }

    /**
     * @param taken  outcome of the repository's {@code existsBy...} check
     * @param field  the request field to blame, e.g. {@code "departmentCode"}; the message
     *               starts with it, which is what the client matches on
     * @param entity the entity as it reads mid-sentence, article included: {@code "a tax"},
     *               {@code "an organization"}
     */
    public static void assertNotTaken(boolean taken, String field, String entity, Object value) {
        assertNotTaken(taken, field, entity, field, value);
    }

    /**
     * As above, for a field whose name reads badly in the sentence - {@code departmentCode}
     * blamed as the field, but described as "code".
     */
    public static void assertNotTaken(boolean taken, String field, String entity, String label, Object value) {
        if (taken) {
            throw new IllegalArgumentException(
                    field + ": " + entity + " with " + label + " '" + value + "' already exists");
        }
    }
}
