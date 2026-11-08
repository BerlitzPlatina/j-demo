package com.example.gacha.banner.entity;

/**
 * Lifecycle of a banner as an operator manages it.
 * <p>
 * Whether a published banner is actually live is not a status: it follows from
 * {@code startTime}/{@code endTime} and the current time, so nothing has to flip a flag when
 * the window opens or closes.
 *
 * <pre>
 * DRAFT --publish--> PUBLISHED --archive--> ARCHIVED
 *   \______________________archive_____________/^
 * </pre>
 */
public enum BannerStatus {
    /** Being prepared; freely editable and deletable, never shown to players. */
    DRAFT,
    /** Visible to players while inside its time window. */
    PUBLISHED,
    /** Retired; read-only and never shown again. */
    ARCHIVED
}
