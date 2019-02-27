/*
 * @(#)SimpleEmailMessage.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.smtp;

import brickst.robocust.lib.SystemConfig;
import brickst.robocust.mime.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * The SimpleEmailMessage class could be used to compose and send a
 * simple email message. It could also be used to redirect a received
 * SmtpMessage.  <p>
 *
 * To use this class:
 * <code>
 *		VArray recipients = new VArray();
 *		recipients.addElement("jim@connectify.com");
 *		recipients.addElement("connectify@aol.com");
 *		String body = "Greetings from xxx";
 *		SimpleEmailMessage message =
 *			new SimpleEmailMessage();
 *		message.setBody(body);
 *		message.setRecipients(recipient);
 *		try {
 *			message.send();
 *		} catch (IOException ex) {
 *			...
 *		}
 * <br>
 *
 * It could also be used to send Html or simple MPA message
 * <code>
 *		...
 *		message.setBody(body);
 *		message.setHtmlBody(htmlBody);
 *		...
 * <br>
 *
 * Concurrency: No synchronization is provided.
 * <br>
 */
public class SimpleEmailMessage extends KcMimeMessage
{
	static Logger logger = Logger.getLogger(SimpleEmailMessage.class);
	static final long serialVersionUID = 8418410621070254779L;

	/**
	 * private variables to determine if this is a redirect mail
	 */
	private boolean isARedirect = false;

	/**
	 * the SMTP server to be used for sending mail; null indicates we should
	 * use the default server.  This allows us to make this value non-transient
	 * so that when the email gets stored and retrieved, we will use the
	 * whatever default SMTP server is configured currently (rather than the one
	 * that was configured when this email was created).  This value is set,
	 * then, only to override permanently the default SMTP server (beware
	 * configuration changes not taking effect on old email objects sitting in
	 * files).
	 */
	private String smtpServer = null;
	/**
	 * the SMTP port to be used for sending mail; 0 indicates we should use the
	 * default port.
	 *
	 * @see SimpleEmailMessage#smtpServer
	 */
	private int smtpPort = 0;

	/**
	 * a SmtpSender used to send this message out
	 */
	private SmtpSender mailSender;

	/**
	 * Construct a emtpy message
	 */
	public SimpleEmailMessage()
	{
		SystemConfig sc = SystemConfig.getInstance();
		String defSender = sc.getProperty(SystemConfig.VC_SENDER_ADDRESS);
		String defSenderName = sc.getProperty(SystemConfig.VC_SENDER_NAME);
		
		setSender(defSender, defSenderName);
	}

	/**
	 * Constructor
	 *
	 * @param msg	a SmtpMessage
	 */
	public SimpleEmailMessage(SmtpMessage msg) { super(msg); }

	/**
	 * Set recipients for this simple email message.
	 *
	 * @param recipients  String VArray of email addresses
	 */
	public void setRecipients(ArrayList<String> recipients) {
		try {
			super.addRecipients(recipients);
		} catch (KcMimeException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	/**
	 * Set redirect recipients. When we set redirect recipients,
	 * we don't change message header.
	 *
	 * @param recipients  String VArray of recipients email addresses
	 */
	public void setRedirectRecipients(ArrayList<String> recipients) {
		String[] recps = new String[recipients.size()];
		recps = recipients.toArray(recps);

		super.setEnvelopeRecipients(recps);
		this.setIsRedirected(true);
	}

	/**
	 * Set sender email address without sender name
	 *
	 * @param  sender	Sender email address
	 */
	public void setSender(String sender)
	{
		setSender(sender, null);
	}

	/**
	 * Set sender email address with sender name
	 *
	 * @param  sender		Sender email address
	 * @param  senderName	display name of sender
	 */
	public void setSender(String sender, String senderName)
	{
		try {
			super.setFrom(sender, senderName);
		} catch (KcMimeException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Return message subject
	 */
	public String getSubject() { return(super.getSubject()); }
	/**
	 * Set message subject
	 */
	public void setSubject(String subject)
	{
		super.setSubject(subject);
	}

	/**
	 * Return message body
	 *
	 * @return String message body
	 */
	public String getBody() {
		KcMimeBodyPart bodyPart = super.getFirstTextBody();
		return(bodyPart.getContent());
	}

	/**
	 * Set message body. We only support content type: text/plain
	 *
	 * @param body  message body
	 */
	public void setBody(String body)
	{
		super.setPlainTextBody(body);
		//"byte-stuffing" occurs in smtp send, NOT here.
	}

	/**
	 * Set Html body.
	 *
	 * @param body  message body
	 */
	public void setHtmlBody(String body)
	{
		super.setHtmlBody(body);
	}

	/**
	 * return smtp server
	 */
	private String getSmtpServer()
	{
		if (smtpServer == null) {
			String simpleEmailServer = 
				SystemConfig.getInstance().getProperty(SystemConfig.SimpleEmailServer);
			return simpleEmailServer;
		}
		else {
			return smtpServer;
		}
	}

	/**
	 * set smtp server
	 */
	public void setSmtpServer(String smtpServer)
	{ this.smtpServer = smtpServer; }

	/**
	 * return smtp port
	 */
	private int getSmtpPort()
	{
		int configSmtpPort = 
			SystemConfig.getInstance().getIntProperty(SystemConfig.SimpleEmailSmtpPort); 

    	assert(configSmtpPort != 0); // "smtpPort is zero for simpleEmailSmtpPort");
		return(smtpPort > 0 ? smtpPort : configSmtpPort);
	}
	/**
	 * Set smtp port
	 */
	public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }

	/**
	 * Set a header field
	 *
	 * @param fieldName  the name of header field
	 * @param fieldValue the value of the field
	 *
	 * @return boolean  true if successfully set the header, false otherwise
	 */
    public void setHeaderField(String fieldName, String fieldValue)
    		throws KcMimeException
    {
    	super.setHeaderField(fieldName, fieldValue);
    }

    /**
     * Send the SimpleEmailMessage out
     *
     * @exception IOException
     */
	public void send() throws IOException
	{

		//check if we have populated recipient, body
		if (!areRecipientsValid())
			complain("Invalid recipients! ");

		try {
			// first establish connection
			connect();
			if (mailSender.sendMessage(this) != SmtpSender.SUCCESS)
				throw new IOException("Failed to send message. " +
									mailSender.getLastError());

		} finally {
			disconnect();
		}
	}

	/**
	 * Writes the complete message to the OutputStream out.
     *
     * @param out the OutputStream to write the message to
     * @exception java.io.IOException
	 */
	public void writeTo(OutputStream out)
		throws java.io.IOException
	{
		super.writeTo(out, isARedirect);
	}

	/**
	 * make connection to SMTP server
	 *
	 * @exception java.io.IOException
	 */
	private void connect()
		throws IOException
	{
		mailSender = new SmtpSender();
		mailSender.setPort(getSmtpPort());
		int connectCode = mailSender.connect(getSmtpServer());

		if (connectCode != SmtpSender.SUCCESS) {
			complain("Failed to connect to server " + getSmtpServer() +
					" Error: " + mailSender.getLastError());		// throws IOException
			disconnect();
		}
	}

	/**
	 * clean out mail sender.  Called from finalize.
	 */
	private void disconnect()
	{
		if (mailSender != null) {
			mailSender.close();
			mailSender = null;
		}
	}

	private boolean getIsRedirected() { return(isARedirect); }
	private void setIsRedirected(boolean flag) { isARedirect = flag; }

	/**
	 * Check if recipients are valid. Call this method before
	 * try to send the message.
	 *
	 * @return boolean  true if recipients are valid, false otherwise
	 */
	public boolean areRecipientsValid()
	{
		String[] recipients = super.getEnvelopeRecipients();

		if (recipients == null || recipients.length == 0)
			return(false);

		// Kludge to shunt sending default address in SysParam.properties
		if (recipients.length == 1) {
			if (recipients[0].equalsIgnoreCase("none"))
				return(false);
		}

		return(true);
	}

	private static void complain(String line)
	    throws IOException
	{
		throw new IOException("Unexpected SMTP server response: " + line);
	}
}
