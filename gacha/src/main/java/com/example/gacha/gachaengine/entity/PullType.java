package com.example.gacha.gachaengine.entity;

/**
 * Types of gacha pulls.
 */
public enum PullType {
    SINGLE("Single pull"),
    MULTI_10("10-pull"),
    MULTI_CUSTOM("Custom multi-pull");

    private final String displayName;

    PullType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
