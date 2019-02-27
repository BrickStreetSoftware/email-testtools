package brickst.robocust.logging;

import brickst.robocust.smtp.SmtpMessage;

/*
 * Records Robocust activity for later action and analysis.
 */
public interface ActivityLogger {
	public int logEmail(SmtpMessage msg);
	public int logHandler(SmtpMessage msg, String handler, String handlerData);
}
