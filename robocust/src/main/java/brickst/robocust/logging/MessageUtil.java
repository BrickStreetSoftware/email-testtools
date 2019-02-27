package brickst.robocust.logging;

import javax.mail.internet.InternetHeaders;
import org.apache.log4j.Logger;
import brickst.robocust.lib.MessageContext;
import brickst.robocust.smtp.SmtpMessage;

public class MessageUtil 
{
	static Logger logger = Logger.getLogger(MessageUtil.class);
	
	/**
	 * Finds Connect Message Context if Email Headers
	 * @param msg
	 * @return MessageContext
	 */
	public static MessageContext getConnectMessageContext(SmtpMessage msg)
	{		
		// find Connect message context in headers
		InternetHeaders hdrs = msg.getHeaders();
		if (hdrs == null) {
			logger.error("cannot parse headers");
			return null;
		}

		MessageContext mc = null;
		String[] mcHdrs = hdrs.getHeader("from");
		if (mcHdrs != null) {
			for (int i = 0; i < mcHdrs.length; i++) {
				mc = MessageContext.parseMessageContext(mcHdrs[i]);
				if (mc != null) {
					break;
				}
			}
		}
		
		if (mc == null) {
			// no message context in from; look in reply-to
			mcHdrs = hdrs.getHeader("reply-to");
			if (mcHdrs != null) {
				for (int i = 0; i < mcHdrs.length; i++) {
					mc = MessageContext.parseMessageContext(mcHdrs[i]);
					if (mc != null) {
						break;
					}
				}			
			}
		}
		
		return mc;
	}
}
