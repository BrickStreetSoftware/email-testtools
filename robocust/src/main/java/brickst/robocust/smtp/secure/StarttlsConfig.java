package brickst.robocust.smtp.secure;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;

import brickst.robocust.lib.SystemConfig;

public class StarttlsConfig {
	public static StarttlsConfig instance;
	public boolean enabled;
	private String algorithm;
	private String keystoreType;
	private String keyStoreFilePath;
	private String keyStoreFilePassword;
	private String protocol;
	
	SSLContext sslContext = null;
	
	static Logger logger = Logger.getLogger(StarttlsConfig.class);

	private StarttlsConfig() {
		SystemConfig config = SystemConfig.getInstance();

		enabled = config.getBooleanProperty(SystemConfig.STARTTLS_ENABLED, true);
		algorithm = config.getProperty(SystemConfig.STARTTLS_ALGORITHM, "SunX509");
		keystoreType = config.getProperty(SystemConfig.STARTTLS_KEYSTORE_TYPE, "PKCS12");
		keyStoreFilePath = config.getProperty(SystemConfig.STARTTLS_KEYSTORE_PATH, "cert/cert.p12");
		keyStoreFilePassword = config.getProperty(SystemConfig.STARTTLS_KEYSTORE_PASSWORD, "password");
		protocol = config.getProperty(SystemConfig.STARTTLS_PROTOCOL, "TLS");
		initSSLContext();
		this.enabled = getSSLContext() != null;
	}
	
	private void initSSLContext() {
        try {
            KeyStore ks = KeyStore.getInstance(this.keystoreType);
            FileInputStream fin = new FileInputStream(this.keyStoreFilePath);
            ks.load(fin, this.keyStoreFilePassword.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(this.algorithm);
            kmf.init(ks, keyStoreFilePassword.toCharArray());
            this.sslContext = SSLContext.getInstance(this.protocol);
            this.sslContext.init(kmf.getKeyManagers(), null, null);
        } catch (Exception e) {
        	logger.info("SSL init error: " + e.getMessage()); 
        }
	}

	public synchronized static StarttlsConfig getInstance() {
		if (instance == null)
			instance = new StarttlsConfig();
		return instance;
	}

	public SSLContext getSSLContext() {
        return this.sslContext;
	}
}
