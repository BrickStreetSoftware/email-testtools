package brickst.robocust.lib;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;

public class SystemConfig extends Properties
{
	static Logger logger = Logger.getLogger(SystemConfig.class);
	private static final long serialVersionUID = -8572652754843783487L;

	// KNOWN PROPERTY NAMES
	public static final String MessageHeaderCharset = "SMTP.MessageHeaderCharset";
	public static final String SimpleEmailSmtpPort = "SMTP.SimpleEmailSmtpPort";
	public static final String SimpleEmailServer = "SMTP.SimpleEmailServer";
	public static final String DefaultSender = "SMTP.DefaultSender";
	public static final String DefaultSenderName = "SMTP.DefaultSenderName";
	
    public static final String NUMBER_OF_TEST_SERVICE = "VC.NumberOfTestService";
    public static final String VC_SERVICE = "VC.service";
    public static final String NAME = "Name";
    public static final String CONFIG = "Config";
    public static final String SERVICE_HANDLER = "ServiceHandler";
    public static final String PERCENT = "percent";
    public static final String SMTP_PORT = "VC.smtpPort";
    public static final String VC_SMTP_SERVER = "VC.SMTP.Server";
    public static final String VC_SMTP_PORT = "VC.SMTP.Port";
    public static final String VC_SMTP_TIMEOUT = "VC.SMTP.Timeout";
    public static final String VC_SENDER_ADDRESS = "VC.SenderAddress";
    public static final String VC_SENDER_NAME = "VC.SenderName";
    public static final String RESPONDER_DELAY_SECONDS = "ResponderDelaySeconds";
    public static final String RESPONDER_MESSAGE_DELAY_SECONDS = "ResponderMessageDelaySeconds";
    public static final String MAX_HANDLEING_THREADS = "VC.maxHandlingThreads";
    public static final String STATS_PERIODICITY_SECONDS = "VC.StatsPeriodicitySeconds";
    public static final String STATS_DISPLAY_ONLY_WHEN_CHANGED = "VC.StatsDisplayOnlyWhenChanged";
	
    public static final String DB_LOG_ENABLED = "VC.DBLog.Enabled";
    public static final String FILE_LOG_ENABLED = "VC.FileLog.Enabled";
    public static final String MSG_LOG_ENABLED = "VC.MessageLog.Enabled";
    
    public static final String STARTTLS_ENABLED = "STARTTLS.enabled";
    public static final String STARTTLS_ALGORITHM = "STARTTLS.algorithm";
    public static final String STARTTLS_KEYSTORE_TYPE = "STARTTLS.keystoreType";
    public static final String STARTTLS_KEYSTORE_PATH = "STARTTLS.keyStoreFilePath";
    public static final String STARTTLS_KEYSTORE_PASSWORD = "STARTTLS.keyStoreFilePassword";
    public static final String STARTTLS_PROTOCOL = "STARTTLS.protocol";
    
	protected Properties props;
	
	private static SystemConfig sysConfig;
	
	public static SystemConfig getInstance()
	{
		return sysConfig;
	}

	public static void init(String filename)
		throws IOException
	{
		if (sysConfig == null) {
			sysConfig = new SystemConfig();
		}
		
		FileInputStream fis = new FileInputStream(filename);
		sysConfig.load(fis);		
	}	
	
	public int getIntProperty(String propertyName)
	{
		return getIntProperty(propertyName, 0);
	}

	public int getIntProperty(String propertyName, int defaultValue)
	{
		String propval = getProperty(propertyName);
		if (propval == null) {
			return defaultValue;
		}
		try {
			int ipropval = Integer.parseInt(propval);
			return ipropval;
		}
		catch (NumberFormatException x) {
			logger.error(x.getMessage(), x);
			return defaultValue;
		}
	}

	public boolean getBooleanProperty(String propertyName)
	{
		return getBooleanProperty(propertyName, false);
	}
	
	public boolean getBooleanProperty(String propertyName, boolean defaultValue)
	{
		String propval = getProperty(propertyName);
		if (propval == null) {
			return defaultValue;
		}

		boolean bpropval = Boolean.parseBoolean(propval);
		return bpropval;
	}

	public double getDoubleProperty(String propertyName)
	{
		return getDoubleProperty(propertyName, 0.0d);
	}
	
	public double getDoubleProperty(String propertyName, double defaultValue)
	{
		String propval = getProperty(propertyName);
		if (propval == null) {
			return defaultValue;
		}

		double dpropval = Double.parseDouble(propval);
		return dpropval;
	}

}
