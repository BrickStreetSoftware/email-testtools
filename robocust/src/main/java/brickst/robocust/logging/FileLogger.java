package brickst.robocust.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.mail.internet.InternetHeaders;

import org.apache.log4j.Logger;

import brickst.robocust.lib.CRMTime;
import brickst.robocust.lib.MessageContext;
import brickst.robocust.smtp.SmtpMessage;

public class FileLogger implements ActivityLogger
{
	//
	// STATIC VARS AND METHODS
	//
	
	static Logger logger = Logger.getLogger(FileLogger.class); 
	public static FileLogger fileLogger = null;
	
	public static FileLogger getInstance()
	{
		if (fileLogger == null) {
			try {
				init();
			}
			catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
		return fileLogger;
	}
	
	public static void init()
		throws IOException
	{
		// XXX
		long ts = System.currentTimeMillis();
		File emailFile = new File("email_log_" + Long.toString(ts) + ".csv");
		File handlerFile = new File("handler_log_" + Long.toString(ts) + ".csv");
		fileLogger = new FileLogger(emailFile, handlerFile);
	}
	
	//
	// LOG FILE INNER CLASS
	//
	class LogFile
	{
		public File logFile;
		public FileOutputStream fileOut;
		public PrintStream printStream;
		public int recordCount;
		
		public LogFile(File _file) 
			throws IOException
		{
			logFile = _file;
			fileOut = new FileOutputStream(logFile, true);
			printStream = new PrintStream(fileOut);
			recordCount = 0;			
		}
		
		public synchronized int log(String msg, boolean flush)
		{
			printStream.println(msg);
			if (flush) {
				printStream.flush();
			}
			recordCount++;
			return recordCount;
		}		
	}
	
	//
	// INSTANCE VARS
	//
	private LogFile emailLog;
	private LogFile handlerLog;
	
	public FileLogger(File emailLogFile, File handlerLogFile) 
		throws IOException
	{
		emailLog = new LogFile(emailLogFile);
		handlerLog = new LogFile(handlerLogFile);
	}
	
	public int logEmail(SmtpMessage msg)
	{
		long logTime = CRMTime.getCurrentMillis();
		
		MessageContext mc = MessageUtil.getConnectMessageContext(msg);
		if (mc == null) {
			logger.error("Unable to Log; No Message Context: " + msg);
			return -1;
		}

		StringBuffer buf = new StringBuffer();
		buf.append(mc.getCustomerId());
		buf.append(',');
		buf.append(mc.getInstanceId());
		buf.append(',');
		buf.append(mc.getEventQueueId());
		buf.append(',');
		buf.append(Long.valueOf(logTime));

		int cc = emailLog.log(buf.toString(), true);
		return cc;
	}

	@Override
	public int logHandler(SmtpMessage msg, String handler, String handlerData) 
	{
		long logTime = CRMTime.getCurrentMillis();
		
		MessageContext mc = MessageUtil.getConnectMessageContext(msg);
		if (mc == null) {
			logger.error("Unable to Log; No Message Context: " + msg);
			return -1;
		}

		StringBuffer buf = new StringBuffer();
		buf.append(mc.getCustomerId());
		buf.append(',');
		buf.append(mc.getInstanceId());
		buf.append(',');
		buf.append(mc.getEventQueueId());
		buf.append(',');
		buf.append(Long.valueOf(logTime));
		buf.append(',');
		buf.append(handler);
		buf.append(',');
		buf.append(handlerData);
		
		int cc = handlerLog.log(buf.toString(), true);
		return cc;		
	}
}
