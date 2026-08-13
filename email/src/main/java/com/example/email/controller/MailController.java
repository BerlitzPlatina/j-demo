package com.example.email.controller;

import com.example.email.dto.AttachmentMailRequest;
import com.example.email.dto.MailRequest;
import com.example.email.dto.ResourceMailRequest;
import com.example.email.service.MailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Map;

/**
 * <p>
 * Mail sending endpoints.
 * </p>
 *
 * @author NamHoang
 */
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Slf4j
public class MailController {

    private final MailService mailService;

    /**
     * Send a plain text mail.
     */
    @PostMapping("/simple")
    public Map<String, Object> sendSimple(@RequestBody MailRequest request) {
        validate(request.to(), request.subject(), request.content());
        mailService.sendSimpleMail(request.to(), request.subject(), request.content(), nullSafe(request.cc()));
        return sent(request.to(), "simple");
    }

    /**
     * Send an HTML mail; {@code content} is interpreted as markup.
     */
    @PostMapping("/html")
    public Map<String, Object> sendHtml(@RequestBody MailRequest request) throws MessagingException {
        validate(request.to(), request.subject(), request.content());
        mailService.sendHtmlMail(request.to(), request.subject(), request.content(), nullSafe(request.cc()));
        return sent(request.to(), "html");
    }

    /**
     * Send an HTML mail with a file attached.
     */
    @PostMapping("/attachment")
    public Map<String, Object> sendAttachment(@RequestBody AttachmentMailRequest request) throws MessagingException {
        validate(request.to(), request.subject(), request.content());
        requireReadableFile(request.filePath(), "filePath");
        mailService.sendAttachmentsMail(request.to(), request.subject(), request.content(), request.filePath(), nullSafe(request.cc()));
        return sent(request.to(), "attachment");
    }

    /**
     * Send an HTML mail with a static resource inlined in the body.
     */
    @PostMapping("/resource")
    public Map<String, Object> sendResource(@RequestBody ResourceMailRequest request) throws MessagingException {
        validate(request.to(), request.subject(), request.content());
        requireReadableFile(request.rscPath(), "rscPath");
        if (!hasText(request.rscId())) {
            throw new IllegalArgumentException("rscId must not be blank");
        }
        mailService.sendResourceMail(request.to(), request.subject(), request.content(), request.rscPath(), request.rscId(), nullSafe(request.cc()));
        return sent(request.to(), "resource");
    }

    private Map<String, Object> sent(String to, String type) {
        log.info("Mail sent [type] = {}, [to] = {}", type, to);
        return Map.of("to", to, "type", type, "sent", true);
    }

    private void validate(String to, String subject, String content) {
        if (!hasText(to)) {
            throw new IllegalArgumentException("to must not be blank");
        }
        if (!hasText(subject)) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        if (!hasText(content)) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    /**
     * The mail service resolves paths on the server's filesystem, so an unreadable path is
     * rejected up front rather than surfacing as a send failure.
     */
    private void requireReadableFile(String path, String field) {
        if (!hasText(path)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        File file = new File(path);
        if (!file.isFile() || !file.canRead()) {
            throw new IllegalArgumentException(field + " is not a readable file: " + path);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * {@code MailService} takes varargs, which reject a null array.
     */
    private String[] nullSafe(String[] cc) {
        return ObjectUtils.isEmpty(cc) ? new String[0] : cc;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleInvalidRequest(IllegalArgumentException e) {
        return Map.of("sent", false, "error", e.getMessage());
    }

    /**
     * Both exception types mean the SMTP conversation failed, which is an upstream problem
     * rather than a bad request.
     */
    @ExceptionHandler({MessagingException.class, MailException.class})
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, Object> handleSendFailure(Exception e) {
        log.error("Sending mail failed", e);
        return Map.of("sent", false, "error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }
}
