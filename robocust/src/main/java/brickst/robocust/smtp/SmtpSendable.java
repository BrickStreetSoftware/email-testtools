/*
 * @(#)SmtpSendable.java	99/12/16
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 * 
 */

package brickst.robocust.smtp;

import java.io.IOException;
import java.io.OutputStream;


/**
 * This interface is used by SmtpSender for sending messages.
 * It has to be implemented for messages that are to be sent
 * using SmtpSender
 * @see   com.kana.connect.common.net.smtp.SmtpSender
 *
 * @author  CJ Lofstedt 12/16/99
 *
 */
public interface SmtpSendable
{
	/**
	 * Returns an array of Strings with recipients for the message.
	 * The recipient email addresses are used for the SMTP "RCPT TO:" command 
	 * and must be in canonical format, i.e. cj@kana.com as specified in
	 * RFC821.
	 */
	public String[] getEnvelopeRecipients();

	/**
	 * Returns a String with the envelope sender for the message.
	 * The sender email address is used for the SMTP "MAIL FROM:" command 
	 * and must be in canonical format, i.e. cj@kana.com as specified in
	 * RFC821.
	 */
	public String getEnvelopeSender();

	/**
	 * Writes the complete message to the CRMOutputStream out.
     *
     * @param out the CRMOutputStream to write the message to
	 */
	public void writeTo(OutputStream out) throws IOException;
}
