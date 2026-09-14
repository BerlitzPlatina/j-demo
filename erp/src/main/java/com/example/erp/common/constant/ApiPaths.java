package com.example.erp.common.constant;

/**
 * Every HTTP entry point of the application, in one place.
 * <p>
 * A controller maps a constant from here instead of a literal, so a path shows
 * up in exactly
 * one file and a rename cannot leave two features disagreeing about the prefix.
 */
public final class ApiPaths {

    /** Prefix every resource below is mounted under. */
    public static final String API = "/api";

    /**
     * Resources that belong to the settings screens rather than to day-to-day data.
     */
    public static final String SETTINGS = API + "/settings";

    public static final String ORGANIZATIONS = API + "/organizations";
    public static final String CONTACTS = API + "/contacts";
    public static final String DEPARTMENTS = API + "/departments";
    public static final String DESIGNATIONS = API + "/designations";
    public static final String TAXES = SETTINGS + "/taxes";
    public static final String LEGAL_ENTITIES = API + "/legal-entities";
    public static final String JOBS = API + "/jobs";

    private ApiPaths() {
    }
}
