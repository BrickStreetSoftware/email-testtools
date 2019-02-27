/**
 * @(#)DsnHardBounceTester.java
 *
 * Copyright (c) 2001 by Kana Software, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;
import brickst.robocust.smtp.*;
import org.apache.log4j.Logger;
import java.io.*;

/**
 * DsnHardBounceTester will generate an hard DSN bounce based 
 * on the SmtpMessage it receives. 
 *
 * @author CJ Lofstedt
 */
public class DsnHardBounceTester extends DefaultReceiverTester
{
	static Logger logger = Logger.getLogger(DsnHardBounceTester.class);

	private String server = SystemConfig.getInstance().getProperty(SystemConfig.VC_SMTP_SERVER);
	private int serverPort = SystemConfig.getInstance().getIntProperty(SystemConfig.VC_SMTP_PORT);
	private String sender = SystemConfig.getInstance().getProperty(SystemConfig.VC_SENDER_ADDRESS);

    public DsnHardBounceTester() {}

    // number of messages sent here defined in vcustomer.properties
    public String handle(SmtpMessage msg)
    {
		DsnMessage bounceMessage = new DsnMessage(sender, msg);
		try
		{
			SmtpSender connection = getConnection();
            if (connection.sendMessage(bounceMessage) != SmtpSender.SUCCESS)
                throw new IOException("VC: Failed to send message. " +
                                    connection.getLastError());
			returnConnection(connection);
			return "hardbounce";
		}
		catch (IOException ex)
		{
            logger.error(ex);
            return "error:" + ex.getMessage();
		}
    }

	private SmtpSender getConnection() throws IOException
	{
		SmtpSender sender = new SmtpSender();
                sender.setPort(serverPort);
		if (sender.connect(server) != SmtpSender.SUCCESS)
                throw new IOException("VC: Failed to connect to server " +
                	server + " Error: " + sender.getLastError());
		return sender;
	}

	private void returnConnection(SmtpSender sender)
	{
		sender.close();
	}
}
