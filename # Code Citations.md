# Code Citations

## POP3Extension.java

- **Connection details from properties:**  
  The POP3 connection details (`username`, `password`, `host`, `port`, `sslEnable`) are injected using Spring's `@Value` annotation and read from `application.properties` or environment variables.  
  _See: `POP3Extension.java`, lines 18–29_

- **POP3 connection setup:**  
  The code sets up the JavaMail `Properties` object using the injected connection details for both `/test` and `/getEmail` actions.  
  _See: `POP3Extension.java`, lines 38–44, 90–96_

- **Attachment base64 encoding:**  
  Attachments are read from the email as streams and encoded to base64 before being added to the response.  
  _See: `POP3Extension.java`, lines 151–162_

- **Body extraction utility:**  
  The email body is extracted using the `extractTextFromMessage` and `extractTextFromMimeMultipart` helper methods to handle both plain text and multipart messages.  
  _See: `POP3Extension.java`, lines 186–210_

## README.md

- **Configuration documentation:**  
  The README documents how to provide POP3 connection details via properties and how the extension expects these values.  
  _See: `README.md`, "POP3 Connection Properties" section_

---