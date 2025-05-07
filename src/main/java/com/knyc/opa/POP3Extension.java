/*
 * Copyright (c) 2023 Ken Ng, Inc. All rights reserved.
 */

package com.knyc.opa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeBodyPart;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;


@Controller
public class POP3Extension {

    // Inject connection details from application.properties or environment variables
    @Value("${pop3.username}")
    private String username;

    @Value("${pop3.password}")
    private String password;

    @Value("${pop3.host}")
    private String host;

    @Value("${pop3.port}")
    private int port;

    @Value("${pop3.ssl_enable:true}")
    private boolean sslEnable;


    @RequestMapping(path = "/test", method = RequestMethod.GET)
    public @ResponseBody Map<String, Object> test() {
        Map<String, Object> responseData = new HashMap<>();
        Properties prop = new Properties();
        prop.put("mail.store.protocol", "pop3");
        prop.put("mail.pop3.username", username);
        prop.put("mail.pop3.password", password);
        prop.put("mail.pop3.host", host);
        prop.put("mail.pop3.port", String.valueOf(port));
        prop.put("mail.pop3.ssl.enable", String.valueOf(sslEnable));

        Session session = Session.getInstance(prop);

        Store store = null;
        Folder emailFolder = null;
        try {
            store = session.getStore("pop3");
            store.connect(host, port, username, password);

            emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            int count = emailFolder.getMessageCount();
            responseData.put("status", "success");
            responseData.put("message", "POP3 connection successful. Message count: " + count);


        } catch (Exception e) {
            responseData.put("status", "error");
            responseData.put("message", e.toString());
        } finally {
            try {
                if (emailFolder != null && emailFolder.isOpen()) emailFolder.close(false);
                if (store != null) store.close();
            } catch (Exception ignore) {}
        }
        return responseData;
    }

    @RequestMapping(path = "/getEmail", method = RequestMethod.POST)
    public @ResponseBody Map<String, Object> getEmail(@RequestBody Map<String, Object> body) {
        Map<String, Object> responseData = new HashMap<>();

        String messageId = (String) body.get("message_id");
        boolean deleteAfterRetrieve = body.get("delete_after_retrieve") != null && Boolean.parseBoolean(body.get("delete_after_retrieve").toString());

        Properties prop = new Properties();
        prop.put("mail.store.protocol", "pop3");
        prop.put("mail.pop3.username", username);
        prop.put("mail.pop3.password", password);
        prop.put("mail.pop3.host", host);
        prop.put("mail.pop3.port", String.valueOf(port));
        prop.put("mail.pop3.ssl.enable", String.valueOf(sslEnable));

        Session session = Session.getInstance(prop);

        Store store = null;
        Folder emailFolder = null;
        try {
            store = session.getStore("pop3");
            store.connect(host, port, username, password);

            emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_WRITE);

            Message[] messages = emailFolder.getMessages();
            Message targetMessage = null;

            // Find by message_id if provided, else get the first unread (oldest)
            if (messageId != null && !messageId.isEmpty()) {
                for (Message msg : messages) {
                    String[] headers = msg.getHeader("Message-ID");
                    if (headers != null && headers.length > 0 && headers[0].equals(messageId)) {
                        targetMessage = msg;
                        break;
                    }
                }
            } else {
                for (Message msg : messages) {
                    if (!msg.isSet(Flags.Flag.SEEN)) {
                        targetMessage = msg;
                        break;
                    }
                }
                if (targetMessage == null && messages.length > 0) {
                    targetMessage = messages[0];
                }
            }

            if (targetMessage == null) {
                responseData.put("status", "success");
                responseData.put("email", null);
                return responseData;
            }

            Map<String, Object> email = new HashMap<>();
            email.put("message_id", getHeader(targetMessage, "Message-ID"));
            email.put("from", ((InternetAddress) targetMessage.getFrom()[0]).getAddress());
            email.put("to", ((InternetAddress) targetMessage.getRecipients(Message.RecipientType.TO)[0]).getAddress());
            email.put("subject", targetMessage.getSubject());
            email.put("date", targetMessage.getSentDate());
            email.put("content_type", targetMessage.getContentType());

            // Handle body and attachments
            String bodyStr = extractTextFromMessage(targetMessage);
            List<Map<String, Object>> attachments = new ArrayList<>();
            Object content = targetMessage.getContent();
            if (content instanceof MimeMultipart) {
                MimeMultipart multipart = (MimeMultipart) content;
                for (int i = 0; i < multipart.getCount(); i++) {
                    MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                        Map<String, Object> attachment = new HashMap<>();
                        attachment.put("filename", part.getFileName());
                        attachment.put("content_type", part.getContentType());
                        // Read the attachment content and encode as base64
                        try (java.io.InputStream is = part.getInputStream()) {
                            byte[] bytes = is.readAllBytes();
                            String base64Content = Base64.getEncoder().encodeToString(bytes);
                            attachment.put("content", base64Content);
                        }
                        attachments.add(attachment);
                    }
                }
            }
            email.put("body", bodyStr);
            email.put("attachments", attachments);

            responseData.put("status", "success");
            responseData.putAll(email);

            if (deleteAfterRetrieve) {
                targetMessage.setFlag(Flags.Flag.DELETED, true);
            }

        } catch (Exception e) {
            responseData.put("status", "error");
            responseData.put("message", e.getMessage());
        } finally {
            try {
                if (emailFolder != null && emailFolder.isOpen()) emailFolder.close(true);
                if (store != null) store.close();
            } catch (Exception ignore) {}
        }
        return responseData;
    }

    private String getHeader(Message message, String headerName) throws MessagingException {
        String[] headers = message.getHeader(headerName);
        return (headers != null && headers.length > 0) ? headers[0] : "";
    }

    private String extractTextFromMessage(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            return extractTextFromMimeMultipart((MimeMultipart) content);
        }
        return "";
    }

    private String extractTextFromMimeMultipart(MimeMultipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/plain")) {
                result.append(part.getContent());
            } else if (part.isMimeType("text/html")) {
                // Optionally, handle HTML differently or skip if you only want plain text
                result.append(part.getContent());
            } else if (part.getContent() instanceof MimeMultipart) {
                result.append(extractTextFromMimeMultipart((MimeMultipart) part.getContent()));
            }
        }
        return result.toString();
    }

}
