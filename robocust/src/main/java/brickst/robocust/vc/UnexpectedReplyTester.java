/**
 * @(#)UnexpectedReplyTester.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.smtp.*;

/**
 * UnexpectedReplyTester will generate an unexpected reply based 
 * on SmtpMessage it receives. 
 *
 * @author Jim Mei
 */
public class UnexpectedReplyTester extends DefaultReceiverTester
{
    public UnexpectedReplyTester()    {    }

    // number of messages sent here defined in vcustomer.properties
    public String handle(SmtpMessage msg)
    {
    	// unexpected reply
    	String body = "This is a spam mail.\n\n"; // + ttsr.message;
    	//String xmailer = "Microsoft Outlook 8.5, Build 4.71.2173.0";
    	SimpleEmailMessage message = setUpMessage(msg.getEnvelopeSender(), body);
    	message.setSender(msg.getEnvelopeRecipients()[0]);
    	message.setSubject("Change");
    	boolean status = send(message);
    	return (status ? "unexpected-reply" : "error");
    }
        
}
