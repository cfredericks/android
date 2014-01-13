package com.cfred1985.util.mail;

import java.io.InputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.IOUtils;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

public class GMailOauthSender
{
    private Session session;

    public SMTPTransport connectToSmtp(String host, int port, String userEmail, String oauthToken,
                    boolean debug) throws Exception
    {
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.sasl.enable", "false");
        session = Session.getInstance(props);
        session.setDebug(debug);

        final URLName unusedUrlName = null;
        SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
        // If the password is non-null, SMTP tries to do AUTH LOGIN.
        final String emptyPassword = null;
        transport.connect(host, port, userEmail, emptyPassword);

        byte[] response = String.format("user=%s\1auth=Bearer %s\1\1", userEmail, oauthToken)
                        .getBytes();
        response = BASE64EncoderStream.encode(response);

        transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);

        return transport;
    }

    public synchronized void sendMail(String from, String subject, String body, String recipients,
                    String user, String oauthToken, InputStream[] attachments)
    {
        try
        {
            SMTPTransport smtpTransport = connectToSmtp("smtp.gmail.com", 587, user, oauthToken,
                            true);

            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(from));
            message.setSender(new InternetAddress(user));
            message.setSubject(subject);

            if (recipients.indexOf(',') > 0)
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            else
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));

            MimeMultipart multipart = new MimeMultipart();

            // Add attachments
            String attachmentText = "";
            if (attachments != null)
            {
                int imageCount = 0;
                for (InputStream attachment : attachments)
                {
                    String imageId = "image" + imageCount;
                    imageCount++;

                    MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                    attachmentBodyPart.setHeader("Content-ID", "<" + imageId + ">");
                    attachmentBodyPart.setDisposition(MimeBodyPart.INLINE);
                    DataSource source = new ByteArrayDataSource(IOUtils.toByteArray(attachment),
                                    "image/jpeg");
                    attachmentBodyPart.setDataHandler(new DataHandler(source));
                    multipart.addBodyPart(attachmentBodyPart);

                    attachmentText += "<img src=\"cid:" + imageId + "\" />";
                    imageCount++;
                }
            }

            body = body.replaceAll("##IMAGES##", attachmentText);

            // Message body
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(body, "text/html");
            multipart.addBodyPart(messageBodyPart);

            message.setContent(multipart);

            smtpTransport.sendMessage(message, message.getAllRecipients());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
