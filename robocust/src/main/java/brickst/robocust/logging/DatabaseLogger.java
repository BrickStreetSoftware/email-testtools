package brickst.robocust.logging;

import brickst.robocust.db.Database;
import brickst.robocust.db.DatabasePoolEntry;
import brickst.robocust.lib.CRMTime;
import brickst.robocust.lib.MessageContext;
import brickst.robocust.smtp.SmtpMessage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.mail.internet.InternetHeaders;

import org.apache.log4j.Logger;

public class DatabaseLogger implements ActivityLogger
{
	static Logger logger = Logger.getLogger(DatabaseLogger.class); 
	private static final String SQL_MSG_EXISTS =
		"select msg_count from customer_queue_vc " +
		"where customer_id = ? and instance_id = ? and event_queue_id = ?";
	private static final String SQL_MSG_LOG_UPDATE =
		"update customer_queue_vc set msg_count = msg_count + 1, update_datetime = ? " +
		"where customer_id = ? and instance_id = ? and event_queue_id = ?";
	private static final String SQL_MSG_LOG_INSERT =
		"insert into customer_queue_vc " +
		"(customer_id, instance_id, event_queue_id, msg_count, update_datetime) " +
		"values (?, ?, ?, 1, ?)";
	
	//
	// STATIC VARS AND METHODS
	//
	public static DatabaseLogger dbLogger = null;
	
	public static DatabaseLogger getInstance()
	{
		if (dbLogger == null) {
			try {
				init();
			}
			catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
		return dbLogger;
	}
	
	public static void init()
		throws IOException
	{
		dbLogger = new DatabaseLogger();
	}

	public DatabaseLogger()
	{
	}
	
	// PreparedStatement cache stored in DatabasePoolEntry
	public static class EmailLoggerStmtCache
	{
		public PreparedStatement psMsgLogExists = null;
		public PreparedStatement psMsgLogUpdate = null;
		public PreparedStatement psMsgLogInsert = null;
	
		public EmailLoggerStmtCache()
		{
		}
		
		public void reset()
		{
			if (psMsgLogExists != null) {
				try {
					psMsgLogExists.close();
				}
				catch (Exception x) {
					// don't care
				}
			}
			if (psMsgLogUpdate != null) {
				try {
					psMsgLogUpdate.close();
				}
				catch (Exception x) {
					// don't care
				}
			}
			if (psMsgLogInsert != null) {
				try {
					psMsgLogInsert.close();
				}
				catch (Exception x) {
					// don't care
				}
			}
		}
		
		public void reset(DatabasePoolEntry dpe)
		{
			reset();
			Database.getInstance().resetConnection(dpe);
		}
	}		
	
	public DatabasePoolEntry getConnection()
	{
		DatabasePoolEntry dpe = Database.getInstance().getConnection();
		if (dpe == null) {
			return null;
		}
		Connection conn = dpe.getConnection();
		EmailLoggerStmtCache psCache = (EmailLoggerStmtCache) dpe.getPoolObject();

		// check valid connection, reset if not
		boolean isConnValid = false;
		try {
			isConnValid = conn.isValid(5);
		}
		catch (SQLException x) {
			logger.error("invalid connection", x);
			isConnValid = false;
		}
		if (! isConnValid) {
			// reset connection
			if (psCache != null) {
				psCache.reset();
				dpe.setPoolObject(null);
			}
			Database.getInstance().resetConnection(dpe);
			return null;
		}
		
		// connection is valid, verify ps cache
		if (psCache == null) {
			psCache = new EmailLoggerStmtCache();
			dpe.setPoolObject(psCache);
		}
		PreparedStatement ps = psCache.psMsgLogExists;
		if (ps == null) {
			try {
				ps = conn.prepareStatement(SQL_MSG_EXISTS);
				psCache.psMsgLogExists = ps;
			}
			catch (SQLException x) {
				logger.error("prepareStatement1", x);
				psCache.reset(dpe);
				return null;
			}			
		}
		ps = psCache.psMsgLogUpdate;
		if (ps == null) {
			try {
				ps = conn.prepareStatement(SQL_MSG_LOG_UPDATE);
				psCache.psMsgLogUpdate = ps;
			}
			catch (SQLException x) {
				logger.error("prepareStatement2", x);
				psCache.reset(dpe);
				return null;
			}			
		}
		ps = psCache.psMsgLogInsert;
		if (ps == null) {
			try {
				ps = conn.prepareStatement(SQL_MSG_LOG_INSERT);
				psCache.psMsgLogInsert = ps;
			}
			catch (SQLException x) {
				logger.error("prepareStatement3", x);
				psCache.reset(dpe);
				return null;
			}			
		}
		
		return dpe;
	}	
	
	public int logEmail(SmtpMessage msg)
	{
		long logTime = CRMTime.getCurrentMillis();
		
		MessageContext mc = MessageUtil.getConnectMessageContext(msg);
		if (mc == null) {
			logger.error("Unable to Log; No Message Context: " + msg);
			return -1;
		}

		//
		// found message context, record in db
		//
		
		DatabasePoolEntry dpe = null;
		int connTries = 0;
		while (true) {
			dpe = getConnection();
			if (dpe != null) {
				break;
			}
			
			// retry or give up?
			if (connTries < 5) {
				logger.info("getConnection returned null, retries: " + connTries);
				connTries++;
				continue;
			}
			else {
				logger.info("getConnection returned null, after " + connTries + " retries, giving up");
				throw new RuntimeException("unable to acquire connection");
			}
		}
		
		try {	// this try..finally block ensures that dpe is returned to the pool
			int msg_count = 0;
			Connection conn = dpe.getConnection();
			EmailLoggerStmtCache psCache = (EmailLoggerStmtCache) dpe.getPoolObject();
			PreparedStatement ps1 = psCache.psMsgLogExists;
			ResultSet rs1 = null;
			try {
				// TODO cache with connections
				ps1.clearParameters(); 
				ps1.setLong(1, mc.getCustomerId());
				ps1.setLong(2, mc.getInstanceId());
				ps1.setLong(3, mc.getEventQueueId());
				rs1 = ps1.executeQuery();
				if (rs1.next()) {
					msg_count = rs1.getInt(1);
				}
			}
			catch (SQLException x) {
				logger.error("check if message exists", x);
				return -1;
			}
			finally {
				try {
					if (rs1 != null) {
						rs1.close();
						rs1 = null;
					}
				}
				catch (Exception x) {
					// don't care
				}
			}
		
			if (msg_count == 0) {
				// insert new record
				ps1 = psCache.psMsgLogInsert;
				try {
					ps1.clearParameters();
					ps1.setLong(1, mc.getCustomerId());
					ps1.setLong(2, mc.getInstanceId());
					ps1.setLong(3, mc.getEventQueueId());
					ps1.setTimestamp(4, new Timestamp(logTime));
					int cc = ps1.executeUpdate();
					if (cc != 1) {
						logger.warn("msg log insert returned " + cc + " for " + mc);
					}
				}
				catch (SQLException x) {
					logger.error("insert message log", x);
					return -1;				
				}
			}
			else {
				// update record
				ps1 = psCache.psMsgLogUpdate;
				try {
					ps1.clearParameters();
					ps1.setLong(1, mc.getCustomerId());
					ps1.setLong(2, mc.getInstanceId());
					ps1.setLong(3, mc.getEventQueueId());
					ps1.setTimestamp(4, new Timestamp(logTime));
					int cc = ps1.executeUpdate();
					if (cc != 1) {
						logger.warn("msg log update returned " + cc + " for " + mc);
					}
				}
				catch (SQLException x) {
					logger.error("update message log", x);
					return -1;				
				}
			}
		}
		finally {
			Database.getInstance().yieldConnection(dpe);
		}
		return 1;
	}


	@Override
	public int logHandler(SmtpMessage msg, String handler, String handlerData) {
		// TODO Auto-generated method stub
		return 0;
	}
}
