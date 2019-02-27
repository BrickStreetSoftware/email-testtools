/**
 * @(#)AutoReplyTester.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.smtp.*;

/**
 * AutoReplyTester will generate an auto reply based 
 * on SmtpMessage it receives. 
 *
 * @author Jim Mei
 */
public class AutoReplyTester extends DefaultReceiverTester
{
    public AutoReplyTester()    {    }

    // number of messages sent here defined in vcustomer.properties
    public String handle(SmtpMessage msg)
    {
    	// Auto reply.
    	String body = "I'm on vacation.\r\n\r\n" + 
    		"***********original message ************\r\n" +
    		new String(msg.getData()); // + ttsr.message;

    	SimpleEmailMessage message = setUpMessage(msg.getEnvelopeSender(), body);
    	message.setSender(msg.getEnvelopeRecipients()[0]);
    	message.setSubject("Auto reply");
        boolean status = send(message);
        
        return (status ? "vacation" : "vacation error");
    }
}
