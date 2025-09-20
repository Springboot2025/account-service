package com.legalpro.accountservice.service.impl;

import com.legalpro.accountservice.service.EmailService;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;

import java.io.IOException;

@Service("sendGridEmailService")
public class SendGridEmailServiceImpl implements EmailService {

    private final SendGrid sendGridClient;
    private final String from = "no-reply@yourdomain.com"; // change as needed

    public SendGridEmailServiceImpl() {
        // ⚠️ Hardcoded API key for testing only
        String apiKey = "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";
        this.sendGridClient = new SendGrid(apiKey);
    }

    @Override
    public void sendEmail(String to, String subject, String bodyHtml) {
        Email fromEmail = new Email(from);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", bodyHtml);
        Mail mail = new Mail(fromEmail, subject, toEmail, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sendGridClient.api(request);

            int status = response.getStatusCode();
            if (status >= 400) {
                throw new RuntimeException("SendGrid API error: status=" + status + ", body=" + response.getBody());
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to send email via SendGrid", ex);
        }
    }
}
