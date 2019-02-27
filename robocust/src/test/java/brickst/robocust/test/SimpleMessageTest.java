package brickst.robocust.test;

import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sun.mail.util.MailSSLSocketFactory;

import brickst.robocust.connection.ServerSocketListener;
import brickst.robocust.connection.ServerSocketProcessor;
import brickst.robocust.lib.SystemConfig;
import brickst.robocust.smtp.SmtpMessage;
import brickst.robocust.smtp.SmtpMessageHandler;
import brickst.robocust.smtp.SmtpReceiver;
import brickst.robocust.smtp.SmtpResponse;

public class SimpleMessageTest {

	private static final String SQL_MSG_EXISTS =
		"select msg_count from customer_queue_vc " +
		"where customer_id = ? and instance_id = ? and event_queue_id = ?";

	boolean dbEnabled;
	boolean fileEnabled;
	String dbURL;
	String dbUser;
	String dbPass;
	String smtpListenHost;
	int smtpListenPort;
	
	@Parameters({ "robocustproperties" })
	@BeforeSuite
	public void setup(String robocustproperties)
	{
		try {
			SystemConfig.init(robocustproperties);
		}
		catch (Exception x) {
			x.printStackTrace();
			System.exit(1);
		}		

		SystemConfig conf = SystemConfig.getInstance();
		dbEnabled = conf.getBooleanProperty("VC.DBLog.Enabled", false);
		fileEnabled = conf.getBooleanProperty("VC.FileLog.Enabled", false);
		dbURL = conf.getProperty("VC.DB.URL");
		dbUser = conf.getProperty("VC.DB.User");
		dbPass = conf.getProperty("VC.DB.Pass");
		smtpListenHost = conf.getProperty("VC.SMTP.Server", "localhost");
		smtpListenPort = conf.getIntProperty("VC.SMTP.Port", 10026);
		
		System.out.println("SETUP:" + this);
		System.out.println("DB Enabled:" + dbEnabled);
		System.out.println("File Enabled:" + fileEnabled);
		System.out.println("DB url:" + dbURL);
		System.out.println("DB user:" + dbUser);
		System.out.println("DB pass:" + dbPass);
		System.out.println("SMTP Port:" + smtpListenPort);

		// set up smtp listener
		ServerSocketListener ssl = new ServerSocketListener(new MySmtpSocketProcessor(), smtpListenHost, smtpListenPort);
		ssl.start();
	}
	
	public class MsgContext
	{
		long customer_id;
		long instance_id;
		long event_queue_id;
		
		public String toString()
		{
			StringBuffer buf = new StringBuffer();
			buf.append("[message-context: ");
			buf.append(customer_id);
			buf.append(".");
			buf.append(instance_id);
			buf.append(".");
			buf.append(event_queue_id);
			buf.append(" ]");
			return buf.toString();
		}
		
		public String toAddress(String prefix, String domain)
		{
			StringBuffer buf = new StringBuffer();
			if (prefix != null) {
				buf.append(prefix);
				buf.append(".");				
			}
			buf.append(Long.toString(customer_id));
			buf.append(".");
			buf.append(Long.toString(instance_id));
			buf.append(".");
			buf.append(Long.toString(event_queue_id));
			buf.append("@");
			buf.append(domain);
			return buf.toString();			
		}
	}
	
	protected boolean checkMsgsSent(Connection conn, ArrayList<MsgContext> msgs)
		throws SQLException
	{
		PreparedStatement ps = conn.prepareStatement(SQL_MSG_EXISTS);
		ResultSet rs = null;
		for (MsgContext msg : msgs) {
			//System.out.println(Thread.currentThread().getName() + ": check " + msg);
			int msg_count = 0;
			ps.clearParameters();
			ps.setLong(1, msg.customer_id);
			ps.setLong(2, msg.instance_id);
			ps.setLong(3, msg.event_queue_id);
			try {
				rs = ps.executeQuery();
				if (rs.next()) {
					msg_count = rs.getInt(1);
				}
			}
			finally {
				if (rs != null) {
					try { rs.close(); } catch (Exception x) { /*donotcare*/ }
				}
			}
			if (msg_count == 0) {
				fail("MSG NOT PRESENT: " + msg);
			}
			if (msg_count > 1) {
				System.out.println(Thread.currentThread().getName() + ": ABNORMAL: msg_count: " + msg_count + " for " + msg);
			}
		}
		return true;
	}
	
	@Parameters({"smtpServer", "smtpPort", "repeat", "starttls"})
	@Test
	public void testSendMsg(String smtpServer, String smtpPort, String repeat, boolean starttls)
		throws MessagingException, UnknownHostException, IOException, GeneralSecurityException
	{
		boolean debug = false;
		int irepeat = 0;
		try {
			irepeat = Integer.parseInt(repeat);
		}
		catch (NumberFormatException x) {
			irepeat = 1;
		}
				
		//Set the host smtp address
		Properties props = new Properties();
		props.put("mail.smtp.host", smtpServer);
		props.put("mail.smtp.port", smtpPort);
		
		if (starttls) {
			MailSSLSocketFactory sf = new MailSSLSocketFactory();
			sf.setTrustAllHosts(true);
			props.put("mail.smtp.ssl.socketFactory", sf);
			props.put("mail.smtp.starttls.enable","true");
			props.put("mail.smtp.starttls.required", "true");
			props.put("mail.smtp.ssl.trust", "*");
		}
		// create some properties and get the default Session
		Session session = Session.getDefaultInstance(props, null);
		session.setDebug(debug);
		Transport transport = session.getTransport("smtp");
		transport.connect();
		
		long startTime = System.currentTimeMillis();
		
		ArrayList<MsgContext> sendlist = new ArrayList<MsgContext>();
		for (int i = 0; i < irepeat; i++) {			
		    // create a message
		    Message msg = new MimeMessage(session);
	
		    // set the from, reply-to, and to address
		    InternetAddress addressFrom = new InternetAddress("postmaster@cmaeda.com");
		    msg.setFrom(addressFrom);
		 
		    // reply-to
		    MsgContext mc = new MsgContext();
		    Random rnd = new Random();
		    mc.customer_id = rnd.nextInt(5000000);
		    mc.instance_id = rnd.nextInt(500000);
		    mc.event_queue_id = 0;
		    
		    msg.addHeader("reply-to", mc.toAddress("kc", "kanaconnect.com"));
	
		    // customer addr
		    InternetAddress[] addressTo = new InternetAddress[1];
		    addressTo[0] = new InternetAddress("customer@example.com");
		    msg.setRecipients(Message.RecipientType.TO, addressTo );
	
		    // Setting the Subject and Content Type
		    msg.setSubject("test subject line");
		    msg.setContent("this is a test", "text/plain");
		    msg.saveChanges();
		    
		    //System.out.println(Thread.currentThread().getName() + ": Send message: " + mc);
		    try {
		    	Thread.sleep(50);
		    	transport.sendMessage(msg, addressTo);
			    sendlist.add(mc);
		    }
		    catch (Exception x) {
				System.out.println(Thread.currentThread().getName() + " " + x.getMessage());
//				x.printStackTrace();		    	
		    }		    
		}
		
		transport.close();
	
		long stopTime = System.currentTimeMillis();
		long elapsed = stopTime - startTime;
		System.out.println(Thread.currentThread().getName() + ": Sent " + irepeat + " msgs in " + elapsed + " ms");
		
	    // check that msg arrived
		if (dbEnabled) {
			System.out.println(Thread.currentThread().getName() + ": Checking send results...");
			Connection conn = null;
			try {
				conn = DriverManager.getConnection(dbURL, dbUser, dbPass);
				checkMsgsSent(conn, sendlist);
			}
			catch (Exception x) {
				x.printStackTrace();
				fail(x.getMessage());
			}	
		}
	}
		
	public class MsgSenderThread implements Runnable
	{
		String smtpServer;
		String smtpPort;
		String repeat;
		boolean starttls;
		
		public MsgSenderThread(String smtpServer, String smtpPort, String repeat, boolean starttls)
		{
			this.smtpServer = smtpServer;
			this.smtpPort = smtpPort;
			this.repeat = repeat;
			this.starttls = starttls;
		}
		
		@Override
		public void run() {
			try {
				testSendMsg(smtpServer, smtpPort, repeat, starttls);
			} catch (Exception e) {
				System.out.println(Thread.currentThread().getName() + " exception ");
//				e.printStackTrace();
			}
		}	
	}
	
	public class MySmtpSocketProcessor implements ServerSocketProcessor
	{
		@Override
		public void handleNewConnection(Socket s) {
			MySmtpMessageHandler handler = new MySmtpMessageHandler();
			handler.rcv = new SmtpReceiver(handler, s);
			Thread th = new Thread(handler);
			th.start();						
		}
	}
	
	public class MySmtpMessageHandler implements SmtpMessageHandler, Runnable
	{
		public SmtpReceiver rcv;

		@Override
		public boolean checkEnvelopeRecipient(String name) {
			// accept all
			return true;
		}

		@Override
		public boolean checkEnvelopeSender(String name) {
			// accept all
			return true;
		}

		@Override
		public SmtpResponse handleMessage(SmtpMessage msg) {
			// log message
			System.out.println("SMTP RECEIVER: RECEIVED MSG");
			System.out.println(msg.toString());
			return SmtpResponse.OK;
		}

		@Override
		public void run() {
			try {
				rcv.receiveMessages();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * @throws MessagingException  
	 */
	@Parameters({"smtpServer", "smtpPort", "repeat", "threads", "starttls"})
	@Test
	public void testMultithreadSendMsg(String smtpServer, String smtpPort, String repeat, String threads, boolean starttls)
		throws MessagingException
	{
		int ithreads = 0;
		try {
			ithreads = Integer.parseInt(threads);
		}
		catch (NumberFormatException x) {
			ithreads = 1;
		}
	
		Thread[] thlist = new Thread[ithreads];
		for (int i = 0; i < ithreads; i++) {
			thlist[i] = new Thread(new MsgSenderThread(smtpServer, smtpPort, repeat, starttls));
		}
		// start threads
		for (int i = 0; i < ithreads; i++) {
			thlist[i].start();
		}
		// wait for completion
		for (int i = 0; i < ithreads; i++) {
			try {
				thlist[i].join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}

	public static void main(String[] args)
	{
		SimpleMessageTest test = new SimpleMessageTest();
		test.setup("robocust.properties");
		try {
			test.testSendMsg("localhost", "10025", "10", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
