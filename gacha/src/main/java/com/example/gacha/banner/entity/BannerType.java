package com.example.gacha.banner.entity;

/**
 * What kind of pool a banner draws from. Stored by name, so reordering the constants is safe;
 * renaming one is a data migration.
 */
public enum BannerType {
    /** The permanent pool, usually without an end time. */
    STANDARD,
    /** A time-limited pool with a featured character. */
    LIMITED,
    /** A time-limited pool with featured weapons/equipment. */
    WEAPON,
    /** A discounted pool for new players, typically capped in pulls. */
    BEGINNER
}
