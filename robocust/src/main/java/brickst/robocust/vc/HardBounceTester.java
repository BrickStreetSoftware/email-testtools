/**
 * @(#)HardBounceTester.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;
import brickst.robocust.smtp.*;

/**
 * HardBounceTester will generate an hard bounce based 
 * on SmtpMessage it receives. 
 *
 * @author Jim Mei
 */
public class HardBounceTester extends DefaultReceiverTester
{
	private String sender = SystemConfig.getInstance().getProperty(SystemConfig.VC_SENDER_ADDRESS);

    public HardBounceTester()  {    }

    // number of messages sent here defined in vcustomer.properties
    public String handle(SmtpMessage msg)
    {
    	// Persistent bounce.
    	String body = "AAAAAAACK! Unknown \nrecipient\r\n\r\n" + 
    		"***********original message ************\r\n" +
    		new String(msg.getData()); // + ttsr.message;
    	SimpleEmailMessage message = setUpMessage(msg.getEnvelopeSender(), body);
    	message.setSender(sender);
    	message.setSubject("AAAACK: Mail Delivery failed!");
	    boolean status = send(message);

	    return (status ? "hardbounce" : "hardbounce error");
    }
}
