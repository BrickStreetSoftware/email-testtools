package brickst.robocust.db;

import java.sql.Connection;

/**
 * Contains a connection pool and an app-specific cache object
 * Database Keeps a Pool of these
 * @author cmaeda
 */
public class DatabasePoolEntry {
	protected Connection conn;
	protected Object poolObject;
		
	public Connection getConnection() {
		return conn;
	}
	public void setConnection(Connection conn) {
		this.conn = conn;
	}
	public Object getPoolObject() {
		return poolObject;
	}
	public void setPoolObject(Object poolObject) {
		this.poolObject = poolObject;
	}

	public DatabasePoolEntry(Connection c, Object item)
	{
		conn = c;
		poolObject = item;
	}	
}
