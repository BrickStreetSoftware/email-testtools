package brickst.robocust.db;

import brickst.robocust.lib.SystemConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import org.apache.log4j.Logger;

/**
 * Manages a lightweight connection pool.
 * Instead of keeping a pool of Connection objects, we keep a pool of DatabasePoolEntry.
 * Callers may store a pool object in the DPE which allows caching of app-specific 
 * per-connection objects such as java.sql.PreparedStatement objects. 
 * @author cmaeda
 *
 */
public class Database {
	static Logger logger = Logger.getLogger(Database.class);
	private static Database instance = null;
	
	public static Database getInstance()
	{
		synchronized (Database.class) {
			if (instance == null) {
				instance = new Database();
			}
		}
		return instance;
	}

	private LinkedList<DatabasePoolEntry> connPool;
	private int maxPoolSize;
	private int currPoolSize;
	private String dbUrl;
	private String dbUser;
	private String dbPass;
	private long poolWaitTimeout;
	
	public Database()
	{
		init();
	}
	
	/**
	 * Configures object from SystemConfig
	 * @see brickst.robocust.lib.SystemConfig
	 */
	protected void init()
	{
		SystemConfig config = SystemConfig.getInstance();
		boolean dbEnabled = config.getBooleanProperty(SystemConfig.DB_LOG_ENABLED, false);
		if (! dbEnabled) {
			throw new RuntimeException("NOT VC.DB.Enabled");
		}
		
		// pool params
		maxPoolSize = config.getIntProperty("VC.DB.MaxPoolSize", 10);
		currPoolSize = 0;
		poolWaitTimeout = config.getIntProperty("VC.DB.WaitTimeout", 5000);
		
		// db url info
		dbUrl = config.getProperty("VC.DB.URL");
		if (dbUrl == null) {
			throw new RuntimeException("NOT VC.DB.URL");
		}
		dbUser = config.getProperty("VC.DB.User");
		dbPass = config.getProperty("VC.DB.Pass");
		
		// init pool
		connPool = new LinkedList<DatabasePoolEntry>();
	}
	
	/**
	 * Get Connection from connection pool
	 * @return connection java.sql.Connection
	 */
	public DatabasePoolEntry getConnection()
	{
		DatabasePoolEntry dpe = null;
		boolean makeNewConn = false;
		
		synchronized (connPool) {
			int cc = 0;
			while (true) {
				cc++;
				if (cc > 0 && (cc % 3) == 0) { 
					// complain a little so we know what is going on
					logger.info("getConnect: trip: " + cc);
				}
				if (connPool.size() > 0) {
					dpe = connPool.remove();
					return dpe;
				}
				else if (currPoolSize < maxPoolSize) {
					currPoolSize++;
					makeNewConn = true;
					break;
				}
				else {
					// wait for free connection
					try {
						connPool.wait(poolWaitTimeout);
					}
					catch (InterruptedException ix) {
						logger.error(ix.getMessage(), ix);
						continue;
					}
				}
			}
		}
		
		// open connection with lock dropped
		// we incremented currPoolSize with the lock
		// so we know that another thread will not open
		// a new connection
		if (makeNewConn) {
			logger.debug("Open New Connection");
			Connection conn = null;
			try {
				conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
			}
			catch (SQLException x) {
				logger.error("getConnection", x);
				return null;
			}
			dpe = new DatabasePoolEntry(conn, null);
		}
		return dpe;
	}
	
	/**
	 * Returns Connection to connection pool
	 * @param conn
	 */
	public void yieldConnection(DatabasePoolEntry dpe) 
	{
		synchronized (connPool) {
			connPool.add(dpe);
			connPool.notify();
		}
	}
	
	// assume caller has cleaned up the pool object
	public void resetConnection(DatabasePoolEntry dpe) 
	{
		if (dpe.conn != null) {
			try {
				dpe.conn.close();
			}
			catch (Exception x) {
				// don't care
			}
		}
		synchronized (connPool) {
			currPoolSize--;
			// notify to allow waiters to create a new connection to replace this one
			connPool.notify();
		}
	}
}
