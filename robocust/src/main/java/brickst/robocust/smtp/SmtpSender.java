/*
 * @(#)SmtpSender.java	99/12/16
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 *
 */

package brickst.robocust.smtp;

import brickst.robocust.lib.*;
//import com.kana.connect.common.sysparam.smtp.MailSenderSuspend;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

/**
 * The SmtpSender class encapsulates the SMTP protocol for sending messages
 * as defined in RFC821.
 * The messages to be sent should implement the SmtpSendable interface.
 * An instantiated SmtpSender object is reusable after close.
 *
 * <P>Sample usage:</P>
 * <pre>
 * SmtpSender smtp = new SmtpSender();
 * int result = smtp.connect("mail.kana.com");
 * if (result == smtp.SUCCESS)
 * {
 *     SmtpSendable message = new TestMessage(...);
 *     result = smtp.sendMessage(message);
 * }
 * if (result != smtp.SUCCESS)
 *     System.out.println("Failed to send message");
 *
 * smtp.close();
 * </pre>
 *
 * @see   com.kana.connect.common.net.smtp.SmtpSendable
 *
 * @author  CJ Lofstedt 12/16/99
 *
 */
public class SmtpSender //extends CRMObject
{
	static Logger logger = Logger.getLogger(SmtpSender.class);

	/** Operation successful */
	public static final int	SUCCESS = 0;
	/** A permanent error occurred that is related to the connection/receiveing SMTP mail host */
	public static final int	PERMANENT_CONNECTION_ERROR = 1;
	/** A temporary error occurred that is related to the connection/receiveing SMTP mail host */
	public static final int	TEMPORARY_CONNECTION_ERROR = 2;
	/** A permanent error occurred that is related to the message recipient */
	public static final int	PERMANENT_RECIPIENT_ERROR = 3;
	/** A temporary error occurred that is related to the message recipient */
	public static final int	TEMPORARY_RECIPIENT_ERROR = 4;
	/** A permanent error occurred that is related to the message recipient, but the recipient address is valid */
	public static final int	SEMIPERMANENT_RECIPIENT_ERROR = 5;

	/** The SMTP port. The assigned port for SMTP is 25. */
	private int port = 25;

	/** Maximum command line length is 512 per RFC */
	private static final int MAX_COMMAND_LINE_LENGTH = 1024;

	/** CR as a byte */
	private static final byte CR_BYTE = 13;
	/** LF as a byte */
	private static final byte LF_BYTE = 10;
	/** CRLF as bytes */
	private static final byte CRLF_BYTES[] = { CR_BYTE, LF_BYTE };

	/** The FQDN (fully qualified doman name) of this host */
	private static String localHostName = null;

	/** time-out value in seconds. (Default timeout is 5 minutes.) */
	private int timeOut = 5*60;

	/** If the SMTP sender should do dot stuffing */
	private boolean dotStuff = true;

	/** TCP socket for the connection */
	private Socket socket = null;

	/** TCP socket InputStream */
	private BufferedInputStream	in = null;
	/** OutputStream with socket write time-out*/
	private OutputStream tout = null;
	/** TCP socket OutputStream */
	private BufferedOutputStream out = null;

	/** Command string buffer */
	private AsciiString command = new AsciiString(50);

	/** Response line string buffer */
	private AsciiString responseLine = new AsciiString(100);

	/** Accumulated response line(s) string buffer (for multiline replies) */
	private AsciiString lastResponse = new AsciiString(200);

	/** last error message or command/reply sequence before the error */
	private String lastError;
	/** Email address of the failing recipient in case of a recipient error */
	private String errorRecipient;

	/** Prefix debug statements so we can track SMTP sessions/conversations */
	private String debugPrefix;
	/** Connection id counter for separating debug statements */
	private int connectionId;
	private static int nextConnectionId = 0;

	/** ESMTP extension 8BITMIME */
	private boolean ext8BitMime = false;
	/** ESMTP extension PIPELINING */
	private boolean extPipelining = false;

	/**
	 * Gets the next connection id.
	 * Each connection gets a unique id, so we can track debug
	 * output per SMTP session.
	 */
	private static synchronized int getNextConnectionId()
	{
		return ++nextConnectionId;
	}

	/**
	 * Constructs a SmtpSender object with default time-out.
	 */
	public SmtpSender()
	{
	}

	/**
	 * Does all necessary SMTP handshaking using an already connected socket,
	 * possibly utilizing available ESMTP extensions.
	 *
     * @param smtpServer the already connected socket to use for the SMTP session
	 *
     * @return the result of the operation which can be one of:
	 *   <pre>
     *   SUCCESS,
     *   TEMPORARY_CONNECTION_ERROR or
     *   PERMANENT_CONNECTION_ERROR
	 *   <pre>
	 */
	public int connect(Socket smtpServer)
	{
		// Assert socket is null
		assert socket == null; //"connect called for an open connection");

		socket = smtpServer;

		connectionId = getNextConnectionId();

		// Construct a prefix for debug statements so we can see which connection the output belongs to
		debugPrefix = socket.getInetAddress().getHostAddress() + "-" + getId() + ": ";

		// Create the input/output streams
        try
        {
			// Set the timeout value (waiting on server responses)
			socket.setSoTimeout(timeOut*1000);

			// Buffered InputStream with default (512) size
			in = new BufferedInputStream(socket.getInputStream());
			// OutputStream with write time-out
			tout = socket.getOutputStream();
			//tout.setTimeout(timeOut*1000);
			// Buffered OutputStream with 2k size
			out = new BufferedOutputStream(tout, 2048);
        }
        catch (IOException e)
        {
			lastError = "Failed connection: " + e.toString();
			logger.debug(debugPrefix + lastError);
            cleanup();
			return TEMPORARY_CONNECTION_ERROR;
        }

		// Handshake with the remote SMTP server
        try
        {
			// First get the greeting
			// Set command to something that makes sense for the error message (getLastError)
			command.set("Waiting for greeting");
			int replyCode = getResponse();
			if (!SmtpReply.isPositive(replyCode))
				return failConnection(replyCode);

			// Helo - try ESMTP EHLO first
			command.set("EHLO ").append(getLocalHostName());
			sendLine(command);
			replyCode = getResponse();
			if (SmtpReply.isPositive(replyCode))
			{
				// Parse out the ESMTP extensions
				String extensions = lastResponse.toString();
				ext8BitMime = (extensions.indexOf("8BITMIME") > -1);
				extPipelining = (extensions.indexOf("PIPELINING") > -1);
			}
			else
			{
				// EHLO failed, try HELO instead
				command.set("HELO ").append(getLocalHostName());
				sendLine(command);
				replyCode = getResponse();
				if (!SmtpReply.isPositive(replyCode))
					return failConnection(replyCode);
			}
        }
        catch (IOException e)
        {
			lastError = "Failed handshake: " + e.toString() + " - " + command;
			logger.debug(debugPrefix + lastError);
            cleanup();
			return TEMPORARY_CONNECTION_ERROR;
        }

		logger.debug(debugPrefix+"SMTP session established. 8BITMIME=" +
								ext8BitMime + " PIPELINING=" + extPipelining);
		return SUCCESS;
	}

	/**
	 * Connects to the SMTP server and does all necessary handshaking,
	 * possibly utilizing available ESMTP extensions.
	 *
     * @param smtpServer the SMTP server to connect to as an InetAddress
	 *
     * @return the result of the operation which can be one of:
	 *   <pre>
     *   SUCCESS,
     *   TEMPORARY_CONNECTION_ERROR or
     *   PERMANENT_CONNECTION_ERROR
	 *   <pre>
	 */
	public int connect(java.net.InetAddress smtpServer)
	{
        Socket sock;

		// Create the connection
        try
        {
			// Connect
            sock = new Socket(smtpServer, port);
        }
        catch (IOException e)
        {
			lastError = "Failed connection: " + e.toString();
			logger.debug(smtpServer.getHostAddress()+ ": " + lastError);
            cleanup();
			return TEMPORARY_CONNECTION_ERROR;
        }

		return connect(sock);
	}

	/**
	 * Connects to the SMTP server and does all necessary handshaking,
	 * possibly utilizing available ESMTP extensions.
	 *
     * @param smtpServer the SMTP server to connect to as a host name or dotted IP address string
	 *
     * @return the result of the operation which can be one of:
	 *   <pre>
     *   SUCCESS,
     *   TEMPORARY_CONNECTION_ERROR or
     *   PERMANENT_CONNECTION_ERROR
	 *   <pre>
	 */
	public int connect(String smtpServer)
	{
		try
		{
			return connect(InetAddress.getByName(smtpServer));
		}
		catch (UnknownHostException e)
		{
			// Bad host name
			lastError = "Failed connection to [" + smtpServer + "] :" + e.toString();
			logger.debug(lastError);
			// REVIEW: For now, assume temporary error here
			return TEMPORARY_CONNECTION_ERROR;
		}
	}

	/**
	 * Sends the message using the already established session.
	 *
     * @param message the message to send. The message should implement the SmtpSendable interface.
	 *
     * @return the result of the operation which can be one of:
	 *   <pre>
     *   SUCCESS,
     *   TEMPORARY_CONNECTION_ERROR,
     *   PERMANENT_CONNECTION_ERROR,
	 *   TEMPORARY_RECIPIENT_ERROR,
	 *   PERMANENT_RECIPIENT_ERROR or
	 *   SEMIPERMANENT_RECIPIENT_ERROR
	 *   </pre>
	 *
	 * @see   com.kana.connect.common.net.smtp.SmtpSendable
	 */
	public int sendMessage(SmtpSendable message)
	{
		// No recipient error
		errorRecipient = null;
		long startTime = CRMTime.getCurrentMillis();
		// Do the mail transaction
		try
		{
			// MAIL FROM
			command.set("MAIL FROM:<").append(message.getEnvelopeSender()).append('>');
			sendLine(command);
			int replyCode = getResponse();
			if (!SmtpReply.isPositive(replyCode))	 // Treat this as a connection error. (Is this always correct?)
				return failConnection(replyCode);

			// RCPT TO
			String[] recipients = message.getEnvelopeRecipients();
			for (int i=0; i<recipients.length; i++)
			{
				command.set("RCPT TO:<").append(recipients[i]).append('>');
				sendLine(command);
				replyCode = getResponse();
				if (!SmtpReply.isPositive(replyCode))
				{
					// Indicate which recipient that caused the failure
					errorRecipient = recipients[i];
					return failRecipient(replyCode);
				}
			}

			// If we are running in test mode, don't send the message, just send a reset(s) instead
/*
			if (MailSenderSuspend.getInstance().getBoolean() == true)
			{
				// We will send two commands so that we get a delay that might be similar to sending the message
				// Send RSET, RSET
				reset();
				reset();

				// Send the message to a null stream
				OutputStream nullOut = new ByteArrayOutputStream(1024);
				try
				{
					message.writeTo(nullOut);
				}
				finally
				{
					nullOut.close();
				}

				logger.debug(debugPrefix+"Message sent in test mode. (Actual message not delivered.)");
				return SUCCESS;
			}
*/
			// DATA
			command.set("DATA");
			sendLine(command);
			replyCode = getResponse();
			if (!SmtpReply.isPositiveIntermediate(replyCode))
				return failRecipient(replyCode);

			// Write the message
			if (dotStuff)
			{
				// Translate LF. to LF.. and lone CR and LF to CRLF
				SmtpOutputStream dotStuffOut = new SmtpOutputStream(out);
				try
				{
					message.writeTo(dotStuffOut);
				}
				finally
				{
					// Free the CRMOutputStream w/o closing the underlying stream
					//dotStuffOut.release();
				}
			}
			else
			{
				message.writeTo(out);
			}

			// Terminate with lone dot on a line (CRLF.CRLF)
		    // This might add an extra blank line to the message... (No big deal for now.)
		    out.write(CRLF_BYTES);
			command.set(".");

			// Temporarily double the time-out value (waiting on server responses)
			// This is to avoid sending duplicate messages. (If we time-out waiting
			// for a response to ".", we don't know if the message is getting
			// delivered or not.)
			socket.setSoTimeout(timeOut*2000);
			//tout.setTimeout(timeOut*2000);

			sendLine(command);
			replyCode = getResponse();

			// Restore the time-out value
			socket.setSoTimeout(timeOut*1000);
			//tout.setTimeout(timeOut*1000);

			long timeElapsed = CRMTime.getCurrentMillis() - startTime;
			if(timeElapsed > (5*60*1000)) //check if it is more than 5 minutes
				logger.debug("Warning: Time elapsed in sending message to SMTP server: "+
						debugPrefix +
						": " + timeElapsed + " msecs");

			if (!SmtpReply.isPositive(replyCode))
				return failRecipient(replyCode);

			logger.debug(debugPrefix+"Message sent successfully");
			return SUCCESS;
		}
        catch (IOException e)
        {
			lastError = e.toString() + " - " + command;
			logger.debug(debugPrefix + lastError);
			long timeElapsed = CRMTime.getCurrentMillis() - startTime;
			if(timeElapsed > (5*60*1000)) //check if it is more than 5 minutes
				logger.debug("Warning: Time elapsed in sending (failed) message to SMTP server: "+
						debugPrefix +
						": " + timeElapsed + " msecs");
            cleanup();
			return TEMPORARY_CONNECTION_ERROR;
        }
	}

	/**
	 * Send a line of ascii text to the OutputStream.
	 * The line gets terminated with CRLF and the OutputStream is flushed.
	 *
     * @param line the line to send
	 */
	private void sendLine(AsciiString line)	throws IOException
	{
		logger.debug(debugPrefix + "C: " + line);
	    line.writeTo(out);
	    out.write(CRLF_BYTES);
		out.flush();
	}

	/**
	 * Reads an SMTP reply.
	 * A reply can be either a single line or multiple lines.
	 * The reply text is built up in the member variable "lastResponse".
	 *
     * @return the SMTP reply code as an integer
	 */
	private int getResponse() throws IOException
	{
		lastResponse.clear();

		// Handle multiline replies
		do
		{
			readLine(responseLine);
			lastResponse.append(responseLine);

			logger.debug(debugPrefix + "S: " + responseLine);

			// Bad server response, has to be at least 3 digits
			if (responseLine.length() < 3)
				// Treat unexpected problem as 451 according to SMTP spec.
				return SmtpReply.CODE_451_ACTION_ABORTED;

			// In case the server just gives us a response code w/o text
			if (responseLine.length() < 4)
				break;

			// Continuation line?
			if (responseLine.charAt(3) == '-')
				lastResponse.append(", ");

			// It is a multiline reply when the result code is followed by a dash.
		} while (responseLine.charAt(3) == '-');

		int replyCode = 100*(responseLine.charAt(0)-'0') +
						10*(responseLine.charAt(1)-'0') +
						(responseLine.charAt(2)-'0');

		return replyCode;
	}

	/**
	 * Abort mail transaction.
	 * This command can also be used to "ping" the remote mail server
	 * to see if it is alive.
	 *
     * @return true if the server is alive and gives a positive response, false otherwise.
	 */
	public boolean reset()
	{
		if (out == null)
			return false;
		try
		{
			// RSET
			command.set("RSET");
			sendLine(command);
			int replyCode = getResponse();
			// Some bad servers (usa.net) reply with 221 when they time out...
			if (replyCode == SmtpReply.CODE_221_CLOSING_TRANSMISSION_CHANNEL)
				return false;
			else
				return SmtpReply.isPositive(replyCode);
		}
		catch (IOException e)
		{
			return false;
		}
	}

	/**
	 * Returns the last (error) response string from the SMTP server.
	 */
	public String getLastError()
	{
		return lastError;
	}

	/**
	 * Returns the email address of the failing recipient in case of a recipient error.
	 * Returns null if no recipient error has occurred.
	 */
	public String getErrorRecipient()
	{
		return errorRecipient;
	}

	/**
	 * Terminates the SMTP session. The object is reusable after close.
	 */
	public void close()
	{
		if (out != null)
		{
			try
			{
				// QUIT
				command.set("QUIT");
				sendLine(command);
				// ignore the reply
				getResponse();
				logger.debug(debugPrefix+"Connection closed");
			}
			catch (IOException e)
			{}

			cleanup();
		}
	}

    private String getThreadId()
    {
        StringBuffer sb = new StringBuffer(Thread.currentThread().getName());
        sb.append("(").append(Thread.currentThread().getId()).append(")");
		return sb.toString();
    }

	/**
	 * Cleans up the session including closing the socket.
	 */
	private void cleanup()
	{
		String thread_id = getThreadId();
		logger.debug(debugPrefix + ": th: " + thread_id + " SmtpSender:cleanup: started cleanup");
		if (in != null)
		{
			try { in.close(); } catch (IOException iox) {}
			in = null;
		}
		if (out != null)
		{
			try { out.close(); } catch (IOException iox) {}
			out = null;
		}
		if (socket != null)
		{
			try { socket.close(); } catch (IOException iox) {}
			socket = null;
		}
		logger.debug(debugPrefix + ": th: " + thread_id + " SmtpSender:cleanup: completed cleanup");
	}

	/**
	 * Sets the timeout value to wait for responses.
	 * Can be called both before and after a connection
	 * has been established.
	 *
	 * @param seconds the TCP read timeout in seconds
	 *
	 * @return true if successful
	 */
	public boolean setTimeout(int seconds)
	{
		if (timeOut != seconds)
		{
			timeOut = seconds;
			if (socket != null)
			{
				// Set the timeout value (waiting on server responses)
				try
				{
					socket.setSoTimeout(timeOut*1000);
					//tout.setTimeout(timeOut*1000);
					logger.debug(debugPrefix + "Changed time-out to: " + timeOut);
				}
				catch (SocketException e)
				{
					logger.debug(debugPrefix + "setTimeout exception: " + e);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Reads a line from the input stream and puts it in the ascii string buffer
     *
     * @param line the AsciiString to hold the line to be read
	 */
	private void readLine(AsciiString line) throws IOException
	{
		line.clear();
		// Read characters (bytes) one by one so that we can determine end-of-line the way we want
		int length = 0;
		int eos_bytes = 0;	// BUGFIX: count end-of-stream bytes read

		// BUGFIX: we are getting NPEs because IN is null
		if (in == null) {
		    String thread_id = getThreadId() ;
		    logger.debug(debugPrefix + ": th: " + thread_id + " SmtpSender:readLine: found null input stream");
		    return;
		}

		while (length <= MAX_COMMAND_LINE_LENGTH)
		{
			byte ch = (byte)in.read();
			// BUGFIX: if EOS is encountered, we should stop reading
			if (ch == -1) {
                eos_bytes++;
                break;
			}
			if (ch == LF_BYTE)
				break;
			if (ch != CR_BYTE)
			{
				line.append(ch);
				length++;
			}
		}

		if (eos_bytes > 0) {
		    logger.debug(debugPrefix + "SmtpSender:LINE2LONG: read " + eos_bytes + " end-of-stream(s)");
		}
		if (length >= MAX_COMMAND_LINE_LENGTH) {
		    logger.debug(debugPrefix + "SmtpSender:LINE2LONG:\"" + line.toString() + "\"");
		    throw new IOException("SmtpSender: Line longer than " + MAX_COMMAND_LINE_LENGTH + " characters");
		}
	}

	/**
	 * Called when a connection oriented failure has occured.
	 * This method will set lastError, cleanup and translate
	 * the SMTP reply code to a SmtpSender.RESULT_* code.
     *
     * @param replyCode the SMTP result from the failed reply
	 */
	private int failConnection(int replyCode)
	{
		// Save off the failed response and command
		lastError = lastResponse + " - " + command;
		logger.debug(debugPrefix + lastError);

		// Close the connection
		close();

		// Is it a permanent error?
		if (SmtpReply.isNegativePermanent(replyCode))
			return PERMANENT_CONNECTION_ERROR;

		// Otherwise treat it as a temporary connection error
		return TEMPORARY_CONNECTION_ERROR;
	}

	/**
	 * Called when a recipient oriented failure has occured.
	 * This method will set lastError, cleanup and translate
	 * the SMTP reply code to a SmtpSender.RESULT_* code.
     *
     * @param replyCode the SMTP result from the failed reply
	 */
	private int failRecipient(int replyCode)
	{
		// Is this a connection related problem (x2z)?
		// This takes care of 421 - Service not available, closing transmission channel
		if (SmtpReply.isCategoryConnection(replyCode))
			return failConnection(replyCode);

		// Save off the failed response and command
		lastError = lastResponse + " - " + command;
		logger.debug(debugPrefix + lastError);

		// Reset
		reset();

		// 552 Requested mail action aborted: exceeded storage allocation
		if (replyCode == SmtpReply.CODE_552_EXCEEDED_STORAGE_ALLOCATION)
			return SEMIPERMANENT_RECIPIENT_ERROR;

		// Is it another permanent error?
		if (SmtpReply.isNegativePermanent(replyCode))
			return PERMANENT_RECIPIENT_ERROR;

		// Otherwise treat it as a temporary recipient error
		return TEMPORARY_RECIPIENT_ERROR;
	}

	/**
	 * Get the host name of this computer.
	 * This should be a FQDN - fully qualified domain name
	 * such as "mail.kana.com".
	 */
	private String getLocalHostName()
	{
		if (localHostName == null)
		{
			// Not been set, create a default
			InetAddress ia = socket.getLocalAddress();
			if (ia != null)
			{
				// Skip the reverse DNS lookup and use domain literal for now
				localHostName = "[" + ia.getHostAddress() + "]";
			}
			else
			{
				// If that fails, use the debug hostname
				try {
					localHostName = InetAddress.getLocalHost().getHostName();
				}
				catch (UnknownHostException x) {
					logger.error("Host Unknown", x);
					localHostName = "localhost";
				}
			}
		}
		return localHostName;
	}

	/**
	 * Set the host name of this computer to be used in EHLO/HELO.
	 * This should be a FQDN - fully qualified domain name
	 * such as "mail.kana.com".
	 * An address literal (such as [1.2.3.4]) is also allowed if the
	 * host doesn't have a name.
	 *
     * @param hostName the FQDN of this host
	 */
	public static void setLocalHostName(String hostName)
	{
		localHostName = hostName;
	}

	/**
	 * Set the remote TCP port to use when initiating a TCP connection to
	 * a remote SMTP server.
	 *
     * @param port the remote TCP port to connect to
	 */
	public void setPort(int port)
	{
		this.port = port;
	}

	/**
	 * If the SMTP sender should perform "dot-stuffing",
	 * i.e. convert the sequence LF. to LF.. so that a
	 * lone dot inside a message body doesn't signal SMTP
	 * end-of-message by mistake.
	 * When this is enabled, lone CR and LF (improper line terminators)
	 * will also be translated to CRLF sequences (proper line terminators).
	 *
	 * Default is dot stuffing turned on (true).
	 *
	 * Only call this method if you are SURE you want to turn it off!
	 *
     * @param dotStuff true if dot-stuffing should be performed, false otherwise.
	 */
	public void doDotStuffing(boolean dotStuff)
	{
		this.dotStuff = dotStuff;
	}

	/**
	 * Returns the connection id.
	 * This is the same id that is being used for SMTP debug output.
	 *
     * @return the id for this connection
	 */
	public int getId()
	{
		return connectionId;
	}

	/**
	 * Finalizer method. Closes connection and cleans up.
	 */
	public void finalize()
	{
		if (in != null || out != null || socket != null) {
			logger.debug(debugPrefix + "SmtpSender:FINALIZE:NON-NULL-RESOURCES:"
				+ " in=" + (in == null ? "null" : in.toString())
				+ " out=" + (out == null ? "null" : out.toString())
				+ " socket=" + (socket == null ? "null" : socket.toString()));
			close();
		}
        /* Removing finalize from superclass because on some systems we're getting OutOfMemoryException
        since the finalization queue is full with objects waiting to be finalized.
        This serves only debug purposes anyway...
		super.finalize();
		*/
	}

	/**
	 * The SmtpReply class contains constants and methods for SMTP reply codes
	 * as specified in RFC821.
	 * Used by SmtpSender.
	 *
	 * @see   com.kana.connect.common.net.smtp.SmtpSender
	 *
	 * @author  CJ Lofstedt 02/03/00
	 */
	static final class SmtpReply
	{
		/** 1yz Positive Preliminary reply */
		private static final int	POSITVE_PRELIMINARY		= 1;
		/** 2yz Positive Completion reply */
		private static final int	POSITVE_COMPLETION		= 2;
		/** 3yz Positive Intermediate reply */
		private static final int	POSITVE_INTERMEDIATE	= 3;
		/** 4yz Transient Negative Completion reply */
		private static final int	NEGATIVE_TRANSIENT		= 4;
		/** 5yz Permanent Negative Completion reply */
		private static final int	NEGATIVE_PERMANENT		= 5;

		/** x2z Reply in connection category */
		private static final int	CATEGORY_CONNECTION		= 2;

		/** 552 Requested mail action aborted: exceeded storage allocation */
		public static final int	CODE_552_EXCEEDED_STORAGE_ALLOCATION = 552;
		/** 221 <domain> Service closing transmission channel */
		public static final int	CODE_221_CLOSING_TRANSMISSION_CHANNEL = 221;
		/**
		 * 451 Requested action aborted: error in processing.
		 * Can be used for example when the server drops the connection
		 * per the SMTP spec.
		 */
		public static final int	CODE_451_ACTION_ABORTED = 451;

		/**
		 * Return true if the reply code is of type 2yz
	     *
	     * @param replyCode the SMTP result code to check
		 */
		public static boolean isPositive(int replyCode)
		{
			return (replyCode/100) == POSITVE_COMPLETION;
		}

		/**
		 * Return true if the reply code is of type 3yz
	     *
	     * @param replyCode the SMTP result code to check
		 */
		public static boolean isPositiveIntermediate(int replyCode)
		{
			return (replyCode/100) == POSITVE_INTERMEDIATE;
		}

		/**
		 * Return true if the reply code is of type 5yz
	     *
	     * @param replyCode the SMTP result code to check
		 */
		public static boolean isNegativePermanent(int replyCode)
		{
			return (replyCode/100) == NEGATIVE_PERMANENT;
		}

		/**
		 * Return true if the reply code is of type x2z
	     *
	     * @param replyCode the SMTP result code to check
		 */
		public static boolean isCategoryConnection(int replyCode)
		{
			return (replyCode % 100)/10 == CATEGORY_CONNECTION;
		}
	}
}
