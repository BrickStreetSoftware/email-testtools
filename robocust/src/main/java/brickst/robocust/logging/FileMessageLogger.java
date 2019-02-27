package brickst.robocust.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.log4j.Logger;

import brickst.robocust.lib.CRMTime;
import brickst.robocust.lib.MessageContext;
import brickst.robocust.smtp.SmtpMessage;

/**
 * Special logger to save incoming message in files.  
 * Used for functional test only; unsuitable for performance testing.
 * @author cmaeda
 */
public class FileMessageLogger
{
	//
	// STATIC VARS AND METHODS
	//
	
	static Logger logger = Logger.getLogger(FileLogger.class); 
	public static FileMessageLogger fileMessageLogger = null;
	
	public static FileMessageLogger getInstance()
	{
		if (fileMessageLogger == null) {
			try {
				init();
			}
			catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
		return fileMessageLogger;
	}
	
	public static void init()
		throws IOException
	{
		fileMessageLogger = new FileMessageLogger();
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
			
	public int logEmail(SmtpMessage msg)
	{
		long logTime = CRMTime.getCurrentMillis();
		
		MessageContext mc = MessageUtil.getConnectMessageContext(msg);
		if (mc == null) {
			logger.error("Unable to Log; No Message Context: " + msg);
			return -1;
		}

		// compute file name
		String fileName = "msg_" + mc.printToString() + "_" + Long.toString(logTime) + ".log";
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileName);
			msg.writeTo(fos);
			return 0;
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		finally {
			if (fos != null) {
				try { fos.close(); } catch (Exception x) {}
			}
		}
	}
}
