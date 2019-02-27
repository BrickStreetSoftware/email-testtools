/**
 * @(#)ReceivedMailTestServiceRequest.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.smtp.*;
import brickst.robocust.service.ServiceRequest;

public class ReceivedMailTestServiceRequest extends ServiceRequest
{
	private static final long serialVersionUID = 4373297061750905913L;
	SmtpMessage message = null;
	
    public ReceivedMailTestServiceRequest(SmtpMessage smtpMessage)
    {
	super("ReceivedMailTest");
		
	message = smtpMessage;
    }

    public SmtpMessage getMessage() { return(message); }
}
