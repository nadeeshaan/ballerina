## Module overview

This module contains functions to perform email operations such as sending and reading emails using SMTP, POP3 and IMAP4 protocols.

## Sample

The sample given below uses the functions in the module to send an email with SMTP protocol.

```ballerina
import ballerina/io;
import ballerina/email;

email:SmtpConfig smtpConfig = {
    connConfig: {
        host: "smtp.gmail.com",
        port: 587,
        username: "sender@email.com",
        password: "pass123"
    }
};

public function main() {
    email:SmtpClient smtpClient = new (smtpConfig);
    email:Email email = {
        toAddr: ["receiver1@email.com", "receiver2@email.com"],
        ccAddr: ["receiver3@email.com", "receiver4@email.com"],
        bccAddr: ["receiver5@email.com"],
        subject: "Sample Email",
        messageBody: "This is a sample email.",
        fromAddr: "sender@email.com",
        senderAddr: "sender@email.com"
    };

    error? dd = smtpClient->send(email);
}
```

The sample given below uses the functions in the module to receive an email with POP3 protocol.

```ballerina
email:PopConfig popConfig = {
    host: "pop.gmail.com",
    username: "sender@email.com",
    password: "senderPa$$w0rd"
};

public function main() {
    email:PopClient popClient = new (popConfig);
    email:Email|error? emailResponse = popClient->read();
    if(emailResponse is email:Email) {
        io:println("Email Subject: ", emailResponse.subject);
        io:println("Email Body: ", emailResponse.body);
    } else if (emailResponse is ()) {
        io:println("There are no emails in the INBOX.");
    } else {
        io:println("Error: " + emailResponse.toString());
    }
}
```

The sample given below uses the functions in the module to receive an email with IMAP4 protocol.

```ballerina
email:ImapConfig imapConfig = {
    host: "imap.gmail.com",
    username: "sender@email.com",
    password: "senderPa$$w0rd"
};

public function main() {
    email:ImapClient imapClient = new (imapConfig);
    email:Email|error? emailResponse = imapClient->read();
    if(emailResponse is email:Email) {
        io:println("Email Subject: ", emailResponse.subject);
        io:println("Email Body: ", emailResponse.body);
    } else if (emailResponse is ()) {
        io:println("There are no emails in the INBOX.");
    } else {
        io:println("Error: " + emailResponse.toString());
    }
}
```
