package com.example.email.dto;

/**
 * <p>
 * Payload for an HTML mail whose body embeds a static resource. The body must reference the
 * resource as {@code <img src="cid:${rscId}">} for the inline part to show up.
 * </p>
 *
 * @author NamHoang
 */
public record ResourceMailRequest(String to, String subject, String content, String rscPath, String rscId,
                                  String[] cc) {
}
