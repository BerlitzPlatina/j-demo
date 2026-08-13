package com.example.email.dto;

/**
 * <p>
 * Payload for a plain text or HTML mail.
 * </p>
 *
 * @author NamHoang
 */
public record MailRequest(String to, String subject, String content, String[] cc) {
}
