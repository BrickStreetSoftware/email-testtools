/*
 * @(#)SmtpMessage.java	1.0 12/16/00
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 * 
 */
package brickst.robocust.smtp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

/**
 * SmtpMessage implements SmtpSendable interface so you can instantiate 
 * an SmtpMessage. It also provides public interface to access/set envelope
 * and data information. SmtpMessage implements SmtpSendable and Serializable. <p>
 *
 * SmtpReceiver will manufacture SmtpMessage and give to the
 * implementor of SmtpMessageHandler. <p>
 * 
 * @see   com.kana.connect.common.net.smtp.SmtpSendable
 * @see   com.kana.connect.common.net.smtp.SmtpReceiver
 * @see   com.kana.connect.common.net.smtp.SmtpMessageHandler
 *
 * @author  Jim Mei
 * @version 2.0 1/19/00
 *
 */
public class SmtpMessage //extends CRMObject
	implements SmtpSendable, Serializable
{
 	/** If you move this file, please consider change this variable */
	static final long serialVersionUID = -1100043774917539312L;

	/** Contains envelop sender(only one sender) information. */
	private String sender;
	/** Contains envelop recipients (multiple recipients) information. */
	private String[] recipients;
	/** message content in a format of byte array */
	private byte[] data;

	private InternetHeaders inetHdrs;
	
	/**
	 * Constructor
	 */
	public SmtpMessage()
	{
	}
	
	public SmtpMessage(SmtpMessage message)
	{
		//keep SmtpMessage information
		//REVIEW: combine the following code with next constructor
		// where it takes a byte[]
		setData(message.getData());
		/* this.setData(message.getData());
		ByteArrayInputStream inputStream = 
				new ByteArrayInputStream(message.getData());
		try	{
			jMessage = new MimeMessage(getSession(), inputStream);
		} catch (MessagingException e) {
			Debug.MIME.println("Exception in KcMimeMessage constructor: " + 
								e + com.brickstreet.common.lib.Util.newLine +
								Util.getStackTrace(e) + com.brickstreet.common.lib.Util.newLine +
								"SmtpMessage: " + message);
		} */
		setEnvelopeSender(message.getEnvelopeSender());
		setEnvelopeRecipients(message.getEnvelopeRecipients());
	}
	
	/** 
	 * Returns Internet Headers
	 */
	public InternetHeaders getHeaders()
	{
		if (inetHdrs == null) {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			try {
				inetHdrs = new InternetHeaders(bais);
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				inetHdrs = null;
			}
		}
		return inetHdrs;
	}
	
	/**
	 * Return envelope recipients. Implementation of SmtpSendable's method. <br>
	 * The recipient email addresses are used for the SMTP "RCPT TO:" command 
	 * (RFC 821) and must be in canonical format. 
	 *
	 * @return String[]  a String array of email address. Null if
	 * we never set this value.
	 */
	public String[] getEnvelopeRecipients() { return recipients; }
	
	/**
	 * Set recipients data. Envelop recipients is data we 
	 * used in "rcpt to:". We may have multiple
	 * recipients for one message. 
	 *
	 * @param recps  String array of a list of email addresses
	 */
	public void setEnvelopeRecipients(String[] recps)
	{
		this.recipients = recps;
	}

	/**
	 * Return envelope sender. Implementation of interface SmtpSendable method. <br>
	 * The sender email address is used for the SMTP "MAIL FROM:" command 
	 * (RFC 821) and must be in canonical format, i.e. cj@kana.com.
	 *
	 * @return String  Email address of sender. Null if we never set this 
	 * value.
	 */
	public String getEnvelopeSender() { return sender; }
	
	/**
	 * Sender is used for the SMTP "mail from:" command. Unlike recipients,
	 * a message has only one sender. This method will set sender information.
	 *
	 * @param sender	email address for sender
	 */
	public void setEnvelopeSender(String sender) { this.sender = sender; }

	/**
	 * Return content of SmtpMessage. In SMTP, after "DATA" command
	 * gets acknowledged, SMTP client (sender) will send message content. <br>
	 *
	 * @return  data in byte array. Return an empty byte array
	 *			if we never set this data.
	 */
	public byte[] getData() { return data; }
	
	/**
	 * Set SmtpMessage's content. In SMTP, after "DATA" command
	 * get acknowledged, SMTP client (sender) will send message content. <br>
	 * In our SmtpMessage, data is in the form of byte array. 
	 *
	 * @param data  raw SMTP data in byte array.
	 */
	public void setData(byte[] data) { this.data = data; }

	/**
	 * Writes the complete message to the OutputStream out.
     *
     * @param out the OutputStream to write the message to
     * @exception java.io.IOException
	 */
	public void writeTo(OutputStream out) 
		throws IOException
	{
		out.write(data);
	}
	
	public String toString()
	{
		StringBuffer sbRecipients = new StringBuffer();
		if (recipients != null)
			for (int i = 0; i < recipients.length; i++)
				sbRecipients.append("<" + recipients[i] + ">, ");
		return("SmtpMessage {\n" +
			   "    sender = <" + sender + ">\n" +
			   "    recipients = " + sbRecipients + "\n" +
			   "    data = " + (data == null ? "null"
											 : ("\n" + new String(data))) +
			   "}\n");
	}
}
