package com.example.email.dto;

/**
 * <p>
 * Payload for an HTML mail carrying a file attachment read from {@code filePath}.
 * </p>
 *
 * @author NamHoang
 */
public record AttachmentMailRequest(String to, String subject, String content, String filePath, String[] cc) {
}
