/*
 * @(#)SmtpResponse.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.smtp;

/**
 * SmtpResponse is a wrapper class used to create an Smtp Response
 *
 *
 * @author Jim Mei	
 * 
 * @see   com.kana.connect.common.net.smtp.SmtpMessage
 *
 */
public class SmtpResponse
{
    private static final int MAX_RESPONSE = 512;
    private int responseCode;
    private String separator = " ";
    private String sResponse; //for performance
    
    /**
    * package level constructor. No one outside this
    * package could instantiate an object except to go to
    * factory interface
    */
    SmtpResponse(int responseCode, String responsePhase)
    {
    	this(responseCode, responsePhase, " ");
    }
    
    SmtpResponse(int responseCode, String responsePhase, String separator)
    {
        //we store response code in case we need to interpret it
        this.responseCode = responseCode;
        //response phase is no longer need, 
        String sResponse = responseCode + separator + responsePhase;
        //sResponse = CRMString.replaceAll(sResponse, "\r", "");
        //sResponse = CRMString.replace(sResponse, "\n", "");
        if (sResponse.length() > 512) 
            this.sResponse = sResponse.substring(0, MAX_RESPONSE);
        else 
            this.sResponse = sResponse;
    }

    /**
     * Create an Response to reject a message with a certain reason
     * @param   reason
     * @return  SmtpResponse
     */
    public static SmtpResponse rejectMessage(String reason)
    {
        return (new SmtpResponse(554, reason));
    }
    
    public String toString() 
    {
        return(sResponse);
    }
        
    /** constants */
    public final static SmtpResponse OK = new SmtpResponse(250, "OK");
    public final static SmtpResponse HELLO = new SmtpResponse(250, "Hello");
    public final static SmtpResponse HELLO_INTERMEDIATE = new SmtpResponse(250, "Hello", "-");
    public final static SmtpResponse STARTTLS = new SmtpResponse(250, "STARTTLS");
    public final static SmtpResponse MESSAGE_ACCEPTED = 
            new SmtpResponse(250, "Message Accepted");
    public final static SmtpResponse COMMAND_UNRECOGNIZED = 
            new SmtpResponse(500, "Syntax error, command " +
            "unrecognized");
    public final static SmtpResponse SYNTAX_ERROR_IN_PARAMETERS =
            new SmtpResponse(501, "Syntax error in parameters "+
            "or arguments");
    public final static SmtpResponse COMMAND_NOT_IMPLEMENTED =
            new SmtpResponse(502, "Command not implemented");
    public final static SmtpResponse BAD_SEQUENCE =
            new SmtpResponse(503, "Bad sequence of commands");
    public final static SmtpResponse COMMAND_PARAMETER_NOT_IMPLEMENTED =
            new SmtpResponse(504, "Command parameter not implemented");
    public final static SmtpResponse SERVICE_READY =
            new SmtpResponse(220, "<domain> Service ready");
    public final static SmtpResponse SERVICE_CLOSING =
            new SmtpResponse(221, "<domain> Service closing transmission channel");
    public final static SmtpResponse SERVICE_NOT_AVAILABLE =
            new SmtpResponse(421, "Service not available, closing transmission channel");
    //     211 System status, or system help reply
    //     214 Help message
    //     251 User not local; will forward to <forward-path>
    //     450 Requested mail action not taken: mailbox unavailable
    //         [E.g., mailbox busy]
    public final static SmtpResponse MAILBOX_UNAVAILABLE =
            new SmtpResponse(550, "Requested action not taken: mailbox unavailable");
    public final static SmtpResponse ERROR_IN_PROCESSING =
            new SmtpResponse(451, "Requested action aborted: error in processing");
    public final static SmtpResponse USER_NOT_LOCAL =
            new SmtpResponse(551, "User not local");
    public final static SmtpResponse INSUFFICIENT_SYSTEM_STORAGE =
            new SmtpResponse(452, "CRequested action not taken: insufficient system storage");
    public final static SmtpResponse EXCEEDED_STORAGE_ALLOCATION =
            new SmtpResponse(552, "Requested mail action aborted: exceeded storage allocation");
    public final static SmtpResponse REQUEST_ACTION_NOT_TAKEN =
            new SmtpResponse(553, "Requested action not taken: mailbox name not allowed");
    public final static SmtpResponse START_MAIL_INPUT =
            new SmtpResponse(354, "Start mail input; end with <CRLF>.<CRLF>");
    public final static SmtpResponse TRANSACTION_FAILED =
            new SmtpResponse(554, "Transaction failed");
  }
