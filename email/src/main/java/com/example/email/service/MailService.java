package com.example.email.service;

import jakarta.mail.MessagingException;

/**
 * <p>
 * Mail service.
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-11-21 11:16
 */
public interface MailService {
    /**
     * Send a plain text mail.
     *
     * @param to      recipient address
     * @param subject mail subject
     * @param content mail body
     * @param cc      carbon copy addresses
     */
    void sendSimpleMail(String to, String subject, String content, String... cc);

    /**
     * Send an HTML mail.
     *
     * @param to      recipient address
     * @param subject mail subject
     * @param content mail body
     * @param cc      carbon copy addresses
     * @throws MessagingException if the mail cannot be sent
     */
    void sendHtmlMail(String to, String subject, String content, String... cc) throws MessagingException;

    /**
     * Send a mail with an attachment.
     *
     * @param to       recipient address
     * @param subject  mail subject
     * @param content  mail body
     * @param filePath attachment path
     * @param cc       carbon copy addresses
     * @throws MessagingException if the mail cannot be sent
     */
    void sendAttachmentsMail(String to, String subject, String content, String filePath, String... cc) throws MessagingException;

    /**
     * Send a mail whose body embeds a static resource.
     *
     * @param to      recipient address
     * @param subject mail subject
     * @param content mail body
     * @param rscPath static resource path
     * @param rscId   static resource id
     * @param cc      carbon copy addresses
     * @throws MessagingException if the mail cannot be sent
     */
    void sendResourceMail(String to, String subject, String content, String rscPath, String rscId, String... cc) throws MessagingException;

}
