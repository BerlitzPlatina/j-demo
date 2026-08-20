package com.example.orm.jpa.dto;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The relations a caller may ask for on a user, i.e. the fetch plan.
 * <p>
 * Every lazy relation that can be exposed gets a constant here, and nothing is ever loaded
 * unless its constant is in the requested set. That is the whole point of keeping the relations
 * {@code LAZY}: what gets fetched is decided per request, in one place, instead of being baked
 * into the mapping.
 */
public enum UserInclude {

    DEPARTMENTS("departments");

    private final String parameter;

    UserInclude(String parameter) {
        this.parameter = parameter;
    }

    public String parameter() {
        return parameter;
    }

    /**
     * Parses the {@code include} query parameter. An unknown name is rejected instead of being
     * ignored, so a typo surfaces as a 400 rather than as silently missing data.
     */
    public static Set<UserInclude> parse(Collection<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return EnumSet.noneOf(UserInclude.class);
        }
        Set<UserInclude> includes = EnumSet.noneOf(UserInclude.class);
        for (String name : requested) {
            includes.add(of(name));
        }
        return includes;
    }

    private static UserInclude of(String name) {
        String normalized = name == null ? "" : name.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(include -> include.parameter.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown include: " + name + ". Allowed: " + allowed()));
    }

    private static String allowed() {
        return Arrays.stream(values()).map(UserInclude::parameter).collect(Collectors.joining(", "));
    }
}
