/**
 * @(#)SoftBounceTester.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;
import brickst.robocust.smtp.*;

/**
 * SoftBounceTester will generate an soft bounce based 
 * on SmtpMessage it receives. 
 *
 * @author Jim Mei
 */
public class SoftBounceTester extends DefaultReceiverTester
{
	private String sender = SystemConfig.getInstance().getProperty(SystemConfig.VC_SENDER_ADDRESS);

    public SoftBounceTester()    {    }

    // number of messages sent here defined in vcustomer.properties
    public String handle(SmtpMessage msg)
    {
    	// soft bounce.
    	String body = "blah blah blah\n\n" + 
	            "***********original message ************\r\n" +
	            new String(msg.getData()); // + ttsr.message;
    	SimpleEmailMessage message = setUpMessage(msg.getEnvelopeSender(), body);
    	message.setSender(sender, "Mail Delivery Subsystem");
    	message.setSubject("AAAACK: Mail Delivery failed!");
	    boolean status = send(message);
	    return (status ? "softbounce" : "softbounce error");
    }
}
