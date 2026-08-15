package com.example.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * <p>
 * The parts of Keycloak's token response this API passes back to the caller.
 * </p>
 *
 * @param accessToken      token to send as {@code Authorization: Bearer ...}
 * @param refreshToken     token used to obtain a new access token without logging in again
 * @param expiresIn        lifetime of the access token, in seconds
 * @param refreshExpiresIn lifetime of the refresh token, in seconds
 * @param tokenType        always {@code Bearer} in practice
 * @author NamHoang
 */
public record TokenResponse(@JsonProperty("access_token") String accessToken,
                            @JsonProperty("refresh_token") String refreshToken,
                            @JsonProperty("expires_in") Long expiresIn,
                            @JsonProperty("refresh_expires_in") Long refreshExpiresIn,
                            @JsonProperty("token_type") String tokenType) {
}
