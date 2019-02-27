/**
 * @(#)ReceiverTester.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.smtp.SmtpMessage;

/**
 * Interface defines the method(s) needs to be implemented by all receiver tester.
 * A receiver tester will receive an VC Message and then generate a reply message
 * based on statistic data. <br>
 *
 * @author Jim Mei
 */
interface ReceiverTester extends IRandom, VcReportProvider
{
    /** 
     * handle VCMessage. 
     * @param msg
     * @return String optional data about how message was handled
     */
    public String handle(SmtpMessage msg);
    
    public String getHandlerName();
}
