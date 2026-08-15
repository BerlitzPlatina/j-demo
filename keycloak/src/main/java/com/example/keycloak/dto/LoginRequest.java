package com.example.keycloak.dto;

/**
 * <p>
 * Credentials posted to the login endpoint.
 * </p>
 *
 * @author NamHoang
 */
public record LoginRequest(String username, String password) {
}
