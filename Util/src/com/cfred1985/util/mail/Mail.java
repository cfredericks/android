package com.cfred1985.util.mail;

import java.io.File;
import java.util.Date;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Mail extends javax.mail.Authenticator
{
    public boolean IsLoggedIn;

    private String user;
    private String password;
    private Session session;

    private final String MessageType = "text/html; charset=utf-8";

    private final String Host = "smtp.googlemail.com";
    private final String Port = "465";
    private final String SPort = "465";

    private final boolean IsAuth = true; // smtp authentication - default on
    private final boolean IsDebuggable = false; // debug mode on or off -
                                                // default off
    private final boolean IsFallback = false;

    public Mail()
    {}

    public Mail(String user, String password)
    {
        Login(user, password);
    }

    public boolean Login(String user, String password)
    {
        this.user = user;
        this.password = password;

        // Setup email properties
        Properties props = new Properties();
        props.put("mail.smtp.host", this.Host);

        if (this.IsDebuggable)
        {
            props.put("mail.debug", "true");
        }

        if (this.IsAuth)
        {
            props.put("mail.smtp.auth", "true");
        }

        props.put("mail.smtp.port", this.Port);
        props.put("mail.smtp.socketFactory.port", this.SPort);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", this.IsFallback ? "true" : "false");

        // There is something wrong with MailCap, javamail can not find a
        // handler for the multipart/mixed part, so this bit needs to be added.
        MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mc);

        // Create email session
        session = Session.getInstance(props, this);

        SetIsLoggedIn();
        return IsLoggedIn;
    }

    public void Logout()
    {
        session = null;
        SetIsLoggedIn();
    }

    public boolean Send(String[] to, String from, String subject, String message,
                    String[] attachments) throws Exception
    {
        if (IsLoggedIn && to.length > 0 && !from.equals("") && !subject.equals("")
                        && !message.equals(""))
        {
            synchronized (session)
            {
                MimeMessage msg = new MimeMessage(session);

                msg.setFrom(new InternetAddress(from));

                InternetAddress[] addressTo = new InternetAddress[to.length];
                for (int i = 0; i < to.length; i++)
                {
                    addressTo[i] = new InternetAddress(to[i]);
                }
                msg.setRecipients(MimeMessage.RecipientType.TO, addressTo);

                msg.setSubject(subject);
                msg.setSentDate(new Date());

                Multipart multipart = new MimeMultipart();

                // Message body
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText(message);
                multipart.addBodyPart(messageBodyPart);

                // Add attachments
                if (attachments != null)
                {
                    for (String attachment : attachments)
                    {
                        BodyPart attachmentBodyPart = new MimeBodyPart();
                        DataSource source = new FileDataSource(attachment);
                        messageBodyPart.setDataHandler(new DataHandler(source));
                        messageBodyPart.setFileName(new File(attachment).getName());

                        multipart.addBodyPart(attachmentBodyPart);
                    }
                }

                // Send email
                msg.setContent(multipart, this.MessageType);
                Transport.send(msg);
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    public void SendAsync(final String[] to, final String from, final String subject,
                    final String message, final String[] attachments)
    {
        new Thread()
        {
            public void run()
            {
                try
                {
                    Send(to, from, subject != null ? subject : "Subject", message != null ? message
                                    : "Body", attachments);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication()
    {
        return new PasswordAuthentication(user, password);
    }

    public String GetPassword()
    {
        return password;
    }

    public String GetUsername()
    {
        return user;
    }

    private void SetIsLoggedIn()
    {
        if (session == null)
            IsLoggedIn = false;

        try
        {
            session.getTransport("smtp").connect();
            IsLoggedIn = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            session = null;
            IsLoggedIn = false;
        }
    }
}