/**
 * @(#)VCLogger.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import org.apache.log4j.Logger;

import brickst.robocust.lib.SystemConfig;
import brickst.robocust.logging.DatabaseLogger;
import brickst.robocust.logging.FileLogger;
import brickst.robocust.logging.FileMessageLogger;
import brickst.robocust.smtp.SmtpMessage;

public class VCLogger
{
	static Logger logger = Logger.getLogger(VCLogger.class);
	private static VCLogger instance = null;
    static boolean dbEnabled = false;    
    static boolean fileEnabled = false;
    static boolean messageEnabled = false;
    
    /**
     * constructor
     */
    private VCLogger()
    {
		SystemConfig config = SystemConfig.getInstance();

		// log activity to db ??
		dbEnabled = config.getBooleanProperty(SystemConfig.DB_LOG_ENABLED, false);
		
		// log activity to file ??
		fileEnabled = config.getBooleanProperty(SystemConfig.FILE_LOG_ENABLED, false);

		// log individual messages to files ??
		messageEnabled = config.getBooleanProperty(SystemConfig.MSG_LOG_ENABLED, false);
    }

    /**
     * getInstance
     */
    public synchronized static VCLogger getInstance() {
        if (instance == null) 
            instance = new VCLogger();
        return instance;
    }
    
    private int countLinkClicked = 0;
    /**
    * log click
    */
    public synchronized void logClick(String link)
    {
        //if (!logToDB) return;
        logCounter(++countLinkClicked, "link clicked: ");
        logger.debug("Link click: " + link);
    }
    	
    private int countMsgRcvd = 0;
    private int countMsgHdlr = 0;
    
    /**
    * log msg received -- called by VcReceiver for all Messages
    */
    public synchronized void logMsgReceived(SmtpMessage msg)
    {
    	logCounter(++countMsgRcvd, "Msg received: ");
        
        // log email to db
        if (dbEnabled) {
        	DatabaseLogger.getInstance().logEmail(msg);
        }
        if (fileEnabled) {
        	FileLogger.getInstance().logEmail(msg);
        }
        if (messageEnabled) {
        	FileMessageLogger.getInstance().logEmail(msg);
        }
    }
    
    public synchronized void logMsgHandler(SmtpMessage msg, String handler, String handlerData)
    {
    	logCounter(++countMsgHdlr, "Msg handled: ");

        // log email to db
        if (dbEnabled) {
        	DatabaseLogger.getInstance().logHandler(msg, handler, handlerData);
        }
        if (fileEnabled) {
        	FileLogger.getInstance().logHandler(msg, handler, handlerData);
        }
    }
    	
    private void logCounter(int counter, String desc)
    {
        if ((counter % 100) == 0) 
            logger.info(desc + " " + counter);
        else if ((counter % 10) == 0)
            logger.debug(desc + " " + counter);
    }

    public int getMsgReceived()
    {
        return countMsgRcvd;
    }
 }
