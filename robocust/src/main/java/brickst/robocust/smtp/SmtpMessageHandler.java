/*
 * @(#)SmtpMessageHandler.java	1.0 2000/1/19
 *
 * Copyright (c) 1999 by Kana Communications, Inc. All Rights Reserved.
 * 
 */
package brickst.robocust.smtp;

import brickst.robocust.smtp.SmtpMessage;

/**
 * SmtpMessageHandler is an interface. It is used by SmtpReceiver 
 * to validata and handler incoming Smtp message. It will also validate 
 * envelope sender, recipient(s). 
 *
 * @see   com.kana.connect.common.net.smtp.SmtpReceiver
 *
 * @author  Jim Mei
 */
public interface SmtpMessageHandler
{
    /**
    * validate sender
    *
    * @return boolean  true if envelope sender is valid
    *				    false if not. Depend on implementation. 
    */
    public boolean checkEnvelopeSender(String name);

    /**
    * validate recipient
    * 
    * @return boolean true if envelope recipient is valid,
    *				   false if not. Depend on implementation. 
    */
    public boolean checkEnvelopeRecipient(String name);

    /**
    * Validate and handle message
    *
    * @param msg	a SmtpMessage to be handled.
    * @return SmtpResponse     a SmtpResponse object
    */
    public SmtpResponse handleMessage(SmtpMessage msg);
}
