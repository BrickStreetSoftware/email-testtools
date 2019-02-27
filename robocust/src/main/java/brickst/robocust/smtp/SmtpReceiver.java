/*
 * @(#)SmtpReceiver.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.smtp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;

import brickst.robocust.lib.CRMTime;
import brickst.robocust.lib.SystemConfig;
import brickst.robocust.smtp.secure.StarttlsConfig;
import brickst.robocust.smtp.secure.StrongTls;

/**
 * SmtpReceiver is using a SMTP socket to receive incoming SmtpMessages. <p>
 *
 * SmtpReceiver only accept msgs with one "rcpt to". This violates
 * RFC, however our Mail Receiver/VC (user of SmtpReceiver) is not 
 * a true MTA and does not anticipate incoming msg 
 * with multiple "rcpt to".  <p>
 *
 * SmtpReceiver will also add a "Received:" header line to start
 * of incoming message data. When SmtpReceiver receives a message,
 * it will label it as received by adding "Received:" header field
 * at the beginning of message content, so that if it redirect/relay 
 * to other MTA, the final receiver could know the path the message
 * passed. <p>
 *
 * @author Jim Mei	01/25/00
 * 
 * @see   com.kana.connect.common.net.smtp.SmtpMessage
 *
 */
public class SmtpReceiver
{
	static Logger logger = Logger.getLogger(SmtpReceiver.class);

    /** constant */
    public final String SMTP_NEW_LINE = "\r\n";
    //change system param name to SMTP.MaxMsgCount
    private static int MAX_MSG_COUNT = 0;

    // state constants
    private static final int WELCOME    =  0;
    private static final int READY      =  1;
    private static final int RECIPIENT  =  2;
    private static final int RECIPIENT1 =  3;
    private static final int QUIT       = -1;

    private Socket socket;
    private PrintWriter		out;			
    private SmtpInputStream 	in;				
    private int		state;				// Set in receiveMessages()
    private String remoteHost;
    private static String  localHost;
    private static String  localIPAddress;
    private String  heloString = "SMTP Receiver ready;";
    private String  sender;
    private String[]  recipients = new String[1];
    private SmtpMessageHandler handler;
    /** If the session should be terminated */
    private boolean	terminate = false;

    /**
    * Constructor. 
    *
    * @param	handler	SmtpMessageHandler
    * @param	socket  a SMTP connection (Socket connection)
    */
    public SmtpReceiver(SmtpMessageHandler handler, Socket socket)
    {
        if (MAX_MSG_COUNT == 0 ) {
            //REVIEW: grab from config file
            MAX_MSG_COUNT = 5000;
        }
        this.handler = handler;
        this.socket = socket;
        if (localHost == null) 
	    setLocalHost(socket.getLocalAddress().getHostName());
        if (localIPAddress == null)
	    setLocalIpAddress(socket.getLocalAddress().getHostAddress());
        remoteHost = socket.getInetAddress().getHostAddress();
    }

    /**
    * Set name of localHost, used in constructing response for "helo"
    *
    * @param localHost  name of localHost
    */
    public void setLocalHost(String localHost) {
        this.localHost = localHost;
    }
    	
    /**
    * Set name of localHost, used in constructing response for "helo"
    * 
    * @param localIPAddress   local IP address
    */
    public void setLocalIpAddress(String localIPAddress) {
        this.localIPAddress = "[" + localIPAddress + "]";
    }
    	
    /**
    * Set heloString
    *
    * @param greeting 
    */
    public void setGreetingString(String greeting)
    {
        this.heloString = greeting;
    }

    /**
    * Main execution method.  It will receive and manufacture
    * SmtpMessages and pass to SmtpMessageHandler for 
    * validation and process. <p>
    *
    * @exception IOException
    */
    public void receiveMessages() throws IOException
    {
        try {
            int timeOut = SystemConfig.getInstance().getIntProperty(SystemConfig.VC_SMTP_TIMEOUT,10*60000); 
            socket.setSoTimeout( timeOut );
            			
            in = new SmtpInputStream(
		        new BufferedInputStream(socket.getInputStream(), 2048 ));
            						
            out = new PrintWriter(socket.getOutputStream());

            respond(new SmtpResponse(220, localHost + " " 
                    + heloString + " "
                    + CRMTime.getCurrentRfc822Date()));

            state = WELCOME;
            int msgCount = 0;

            // Accept at most MR.MaxMsgCount (eg, 100) messages per socket connection.
            // This will help prevent denial of service attacks.
            while (state != QUIT && msgCount < MAX_MSG_COUNT && !terminate) {
                String line = in.readLine();

                if (line == null)	// REVIEW: David added due to exception
                    break;

                String command;
                if (line.length() >= 4)
                    command = line.substring(0,4);
                else
                    command = line;

                if (command.equalsIgnoreCase("QUIT")) {
                    respond(new SmtpResponse(221, localHost + " closing connection."));
                    state = QUIT;
                    terminate();

                } else if (command.equalsIgnoreCase("HELO")
		                || command.equalsIgnoreCase("EHLO")) {
                    if (state == WELCOME) {
                    	if (StarttlsConfig.getInstance().enabled) {
                    		respond(SmtpResponse.HELLO_INTERMEDIATE, SmtpResponse.STARTTLS);
                    	} else {
                    		respond(SmtpResponse.HELLO);
                    	}
	                    state = READY;
                    } else {
                    	logger.debug("BAD_SEQUENCE " + line);
	                    respond(SmtpResponse.BAD_SEQUENCE);
                    }

                } else if (command.equalsIgnoreCase("MAIL")) {
                    msgCount++;
                    handleMail(line);

                } else if (command.equalsIgnoreCase("RCPT")) {
                    handleRcpt(line);

                } else if (command.equalsIgnoreCase("DATA")) {
                    handleData();

                } else if (command.equalsIgnoreCase("RSET")) {
                    state = READY;
                    respond(new SmtpResponse(250, "State reset."));

                } else if (command.equalsIgnoreCase("NOOP")) {
                    respond(SmtpResponse.OK);

                } else if (command.equalsIgnoreCase("HELP")) {
                    printHelp(line);
                } else if (command.equalsIgnoreCase("STAR")) {
                	SSLSocket sslSocket = createSSLSocket(socket);
                	respond(new SmtpResponse(220, "Ready to start TLS"));
                	sslSocket.startHandshake();
                	initStreams(sslSocket);
                	state = WELCOME;
                	logger.info("switched to ssl socket");
                } else {
                    // SEND, SAML, VRFY, EXPN, TURN 
                	logger.debug("COMMAND_UNRECOGNIZED " + line);
                    respond(SmtpResponse.COMMAND_UNRECOGNIZED);
                }
            } // (state != QUIT)
            			
            // clean up
            out.flush();
        } catch (IOException ie) {
            logger.debug("Remote host: " +
			            remoteHost +
			            ", state: " + state +		
			            ", exception: " + ie );
            throw ie;
        } finally {
            close();
        }

        logger.debug("Mail session for " + remoteHost + " terminated.");
    }
    
    private void initStreams(Socket socket) throws IOException
    {
        in = new SmtpInputStream(socket.getInputStream());
        out = new PrintWriter(socket.getOutputStream());
        this.socket = socket;
    }
    
    public SSLSocket createSSLSocket(Socket socket) throws IOException
    {
    	SSLContext serverContext = StarttlsConfig.getInstance().getSSLContext();
        SSLSocketFactory sf = serverContext.getSocketFactory();
        InetSocketAddress remoteAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
        
        SSLSocket sslSocket = (SSLSocket) (sf.createSocket(socket, remoteAddress.getHostName(), socket.getPort(), true));
        sslSocket.setEnabledProtocols(sslSocket.getSupportedProtocols());
        sslSocket.setEnabledCipherSuites(StrongTls.intersection(StrongTls.ENABLED_CIPHER_SUITES, sslSocket.getSupportedCipherSuites()));
        
        sslSocket.setUseClientMode(false);
        
        return sslSocket;
    }
    
    /**
    * Closes down the current connection.
    * This will abort any transaction in progress!
    * Use with caution.
    */
    public void terminate()
    {
    	logger.debug("Set session for terminate for " + remoteHost + " terminated."); 
        terminate = true;
    }

    /**
    * private class, close open connections. <p>
    * SmtpReceiver may have three open connections, <br>
    * out: PrintWriter for output <br>
    * in: SmtpInputStream for input <br>
    * socket: socket that connect us to the original SMTP connection <br>
    */
    private void close() 
    {
		if (out != null) {
			out.close(); out = null;
		}
        in.close(); in = null;
		if (socket != null) {
			try {
				socket.close(); 
			}
			catch (IOException iox) {}
			socket = null;
		}
    }

    /**
    * Private method to respond a SMTP command
    *
    * @param response  Response message. For example: 
    *					"250 State reset."
    */
    private void respond(SmtpResponse... responses)
    {
    	for (SmtpResponse response: responses) {
	    	logger.debug(response);
	    	out.print(response);
	    	out.print(SMTP_NEW_LINE);
    	}
    	out.flush();
    }

    /**
    * private method handle "mail from"
    *
    * @param  line  a SMTP command line starts with "mail from" 
    */
    private void handleMail(String line)
    {
        if (state == READY) {
            if (line.length() < 10 ||
	        !line.substring(0, 10).equalsIgnoreCase("MAIL FROM:")) {
	            respond(SmtpResponse.SYNTAX_ERROR_IN_PARAMETERS);
	            return;
            }

            sender = getPath(line.substring(10));
            logger.debug("Sender: " + sender);
            if (handler.checkEnvelopeSender(sender)) {
	            state = RECIPIENT;
	            respond(new SmtpResponse(250, "Sender accepted"));
	            sender = sender.trim();
            } else {
	            respond(new SmtpResponse(550, "Sender rejected"));
            }
        } else {
            respond(SmtpResponse.BAD_SEQUENCE);
        }
    } // handleMail 

    /**
    * private method handle "rcpt to"
    *
    * @param line  a SMTP command line starts with "rcpt to"
    */
    private void handleRcpt(String line)
    {
        if (state == RECIPIENT1) {
            respond(new SmtpResponse(550, "Recipient rejected. " +
		            "Multiple recipient not allowed."));		
        } else if (state == RECIPIENT) {
            if (line.length() < 8   ||
	        !line.substring(0,8).equalsIgnoreCase("RCPT TO:")) {
	            respond(SmtpResponse.SYNTAX_ERROR_IN_PARAMETERS);
	            return;
            }

            recipients[0] = getPath(line.substring(8).trim());
            			
            logger.debug("Recipient: " + recipients[0] );
            if (handler.checkEnvelopeRecipient(recipients[0])) {
	            respond(new SmtpResponse(250, "OK recipient accepted"));
	            state = RECIPIENT1;
	            // Note: change in state, multiple recipient is not allowed.
            } else {
	            respond(new SmtpResponse(550, "Recipient rejected"));
            }
        } else {
            respond(SmtpResponse.BAD_SEQUENCE);
        }
    } // handleRcpt
    	
    /**
    * Get email address (path). In RFC 821, path is in between "<" and ">".
    *
    * @return String  email address (could be empty string ""), return null
    *				   if there's syntax error
    */
    private String getPath(String rawPath)
    {
        if (rawPath.startsWith("<") && rawPath.endsWith(">"))
            return rawPath.substring(1, rawPath.length() -1).trim();
        else
            return rawPath;
    }		

    /**
    * private method handle "data". <p>
    *
    * It will read data from input stream until it hit 
    * "<CRLF>.<CRLF>"; add "Received: from..." header 
    * in front of the content. <p>
    *
    */
    private void handleData() throws IOException
    {
        if (state == RECIPIENT1) {
            respond(SmtpResponse.START_MAIL_INPUT);
            			
            //read data
            byte[] data = in.readData();
            			
            //append receivedField
            String receivedField = "Received: from " + sender + 
		            " by " + this.localHost + " " +
		            this.localIPAddress + " ; " + 
		            CRMTime.getCurrentRfc822Date() +
		            SMTP_NEW_LINE;
            byte[] receiveFieldBytes = receivedField.getBytes();
            byte[] msgBytes = new byte[data.length + receiveFieldBytes.length];
            			
            System.arraycopy(receiveFieldBytes,0, 
					        msgBytes, 0, receiveFieldBytes.length);
            System.arraycopy(data,0, 
					        msgBytes, receiveFieldBytes.length, data.length);

            SmtpMessage msg = new SmtpMessage();
            //add sender
            msg.setEnvelopeSender(sender);
            //add recipient
            msg.setEnvelopeRecipients(recipients);
            			
            //add call back to handle message
            msg.setData(msgBytes);
            			
            SmtpResponse response = this.handler.handleMessage(msg) ;
            respond(response);
            state = READY;
        } else {
            respond(SmtpResponse.BAD_SEQUENCE);
        }
    }  //handleData

    /**
    * private method, handle "help". <br>
    * Print help for SMTP session.
    *
    * @param line  an input SMTP command line starts with "help"
    */
    private void printHelp(String line)
    {
        if (line.equalsIgnoreCase("HELP")) {
	        respond(new SmtpResponse(250, "HELO EHLO MAIL QUIT RCPT DATA"));
	        return;
        }

        String command = line.substring(5);

        if (command.equalsIgnoreCase("HELO"))
            respond(SmtpResponse.HELLO);
        else if (command.equalsIgnoreCase("EHLO"))
            respond(SmtpResponse.HELLO);
        else if (command.equalsIgnoreCase("MAIL"))
            respond(new SmtpResponse(250, "MAIL FROM:<reverse-path>"));
        else if (command.equalsIgnoreCase("RCPT"))
            respond(new SmtpResponse(250, "RCPT TO: <forward-path>"));
        else if (command.equalsIgnoreCase("QUIT"))
            respond(new SmtpResponse(250, "QUIT"));
        else if (command.equalsIgnoreCase("DATA"))
            respond(new SmtpResponse(250, "DATA"));
        else
	    respond(new SmtpResponse(500, "command \"" + command + 
		        "\" unrecognized"));
    } // printHelp
}
