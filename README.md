# Workato POP3 Extension

## Building extension

Steps to build an extension:

1. Install the latest Java 17 SDK
2. Use `./gradlew jar` command to bootstrap Gradle and build the project.
3. The output is in `build/libs`.

## Installing the extension to OPA

1. Add a new directory called `ext` under the Workato agent install directory.
2. Copy the extension JAR file to the `ext` directory. Pre-built jar: [workato-pop3-connector-1.0.0.jar](build/libs/workato-opa-pop3-extension-1.0.0.jar)
3. Also include the following dependencies in the `ext` directory:
   - [jakarta.mail jar](https://mvnrepository.com/artifact/com.sun.mail/jakarta.mail)
   - [angus-activation-2.0.2.jar](https://repo1.maven.org/maven2/org/eclipse/angus/angus-activation/2.0.2/angus-activation-2.0.2.jar)
   - [angus-mail-2.0.3.jar](https://repo1.maven.org/maven2/org/eclipse/angus/angus-mail/2.0.3/angus-mail-2.0.3.jar)
4. Update the `config/config.yml` to add the `ext` directory to the class path.

```yml
server:
   classpath: /opt/opa/workato-agent/ext
```
**
5. Update the `conf/config.yml` to configure the new extension.

```yml
extensions:
   pop3:
      controllerClass: com.knyc.opa.POP3Extension
      mail.pop3.username: your_username
      mail.pop3.password: your_password
      mail.pop3.host: pop3.example.com
      mail.pop3.port: 995
      mail.pop3.ssl_enable: true
```

## Custom SDK for the extension

The corresponding custom SDK can be found here in this repo as well.

Link: [opa-smtp-connector.rb](custom-sdk/opa-smtp-connector.rb)

Create a new Custom SDK in your Workato workspace and use it with the OPA extension.

---

**Note:**  
- This extension is for POP3 only. SMTP (send email) functionality has been removed.
- Make sure to use the latest `jakarta.mail` library instead of `javax.mail`.