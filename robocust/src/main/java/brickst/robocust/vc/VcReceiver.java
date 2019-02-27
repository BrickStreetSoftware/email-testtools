/*
 * @(#)VcReceiver.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;
import brickst.robocust.smtp.*;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import org.apache.log4j.Logger;

/**
 * The VcReceiver contains a SMTP Receiver to receive/validate/process incoming
 * messages. Messages should be sent from Mail Composer.  <p>
 *
 * Alternatively, it could be cached by the ServiceQueue used by the
 * VirtualCustomerServiceProvider.  This would mirror the MailSender model (not yet
 * implemented).
 */
public final class VcReceiver extends Thread
	implements SmtpMessageHandler 
{
	static Logger logger = Logger.getLogger(VcReceiver.class);
    
	private Socket socket;
	private SmtpReceiver smtpReceiver;

    private static int delaySeconds;
    private static int messageDelaySeconds;
    private static double failOverRatio = 0.0;
    	
    // Throttle simultaneous connections
    private static Semaphore handlingThreadThrottle = null;

    // should be called once at init time
    public synchronized static void init()
    {
        //REVIEW: fetch from config
        
        failOverRatio = 0;
        //VCConfig.getInstance().getDouble("VC.DMSFailOverRatio");

        SystemConfig sc = SystemConfig.getInstance();
        
        /* Delay for each new connection */
        delaySeconds = sc.getIntProperty(SystemConfig.RESPONDER_DELAY_SECONDS);
        /* Delay for each message */
        messageDelaySeconds = sc.getIntProperty(SystemConfig.RESPONDER_MESSAGE_DELAY_SECONDS);

        int maxThreads = sc.getIntProperty(SystemConfig.MAX_HANDLEING_THREADS);
        handlingThreadThrottle = new Semaphore(maxThreads, true);

        logger.info("VcReceiver initialized: VC.DMSFailOverRatio=" + failOverRatio +
                ", ResponderDelaySeconds=" + delaySeconds +
                ", ResponderMessageDelaySeconds=" + messageDelaySeconds +
                ", VCLoggerThrottle=" + handlingThreadThrottle);
    }
    
    /**
    * Constructor: Creates and initializes a VcReceiver object
    */
    public VcReceiver(Socket s)
    {
    	socket = s;
        smtpReceiver = new SmtpReceiver(this, s);
    }

    /**
    * Main thread execution method.  NB: This class no longer extends
    * CRMThread.  We should change this class not to claim to be a thread!!!
    */
    public void run()
    {
    	InetAddress sockAddr = socket.getInetAddress();
    	int sockPort = socket.getPort();
    	
    	logger.info("Start Receiver for " + sockAddr + ":" + sockPort);
    	
    	/* Add a delay for each new connection */
        if (delaySeconds > 0) {
        	delay(delaySeconds);
        }

        try {
            smtpReceiver.receiveMessages();
        }
        catch (SocketTimeoutException stx) {
        	// these occur frequently and don't need full stack traces
        	logger.info("Connection timeout: " + sockAddr + ":" + sockPort);
        }
        catch (IOException ex) { 
            logger.error("During smtp connection", ex);
        } 
        finally {
        	try {
        		if (socket != null && ! socket.isClosed()) {
        			socket.close();
        		}
        	}
        	catch (Exception x) {
        		logger.error("Closing socket", x);
        	}
        }
        logger.debug("End Receiver for " + sockAddr + ":" + sockPort);
    }
    	
    /**
    * validate sender
    *
    * @return boolean  true if envelope sender is valid
    *				    false if not. Depend on implementation. 
    */
    public boolean checkEnvelopeSender(String name) 
    {
        if (name == null || name.trim() == "") return false; //vc receive from MC, it should have sender

        /* Add a delay for each message */
        if (messageDelaySeconds > 0) {
        	delay(messageDelaySeconds);
        }

        /*
        ** This is a requested feature.
        ** We want to simulate the DMS fail over due to network problems
        ** or other problems in the real world.  So, we call the terminate()
        ** which terminates the looping of MAX_MSG_COUNT in SmtpReceiver.java
        ** 
        */
        if (Math.random() < failOverRatio) {
            smtpReceiver.terminate();
        }
            		
        return true;
    }

    /**
    * validate recipient
    * 
    * @return boolean true if envelope recipient is valid,
    *				   false if not. Depend on implementation. 
    */
    public boolean checkEnvelopeRecipient(String name) 
    {
        if (name == null || name.trim() == "") {
        	return false;
        }
        return true;
    } 

    /**
    * Validate and handle message
    *
    * @param msg	a SmtpMessage to be handled.
    * @return SmtpResponse     MESSAGE_ACCEPTED 
    */
    public SmtpResponse handleMessage(SmtpMessage smtpMsg) 
    {    	
    	VCLogger.getInstance().logMsgReceived(smtpMsg);            	

    	// Limit number of db connections
        // Debug.VC.println("LoggerThrottle: " + loggerThrottle);
    	handlingThreadThrottle.acquireUninterruptibly();

        //VC.incrementMessagesReceived();

    	try {
            ArrayList<VcService> testServices = VC.getTestServices();
            VcService ts = (VcService) VC.chooseARandom(testServices);
            if (ts != null) {
            	ts.request(smtpMsg);
            }  
        }
        finally {
        	handlingThreadThrottle.release();
        }

        return SmtpResponse.MESSAGE_ACCEPTED;
    }

    /**
    * Delay a random time between 0 and "seconds" seconds. 
    * (Configurable in vcustomer.properties)
    */
    private void delay(int seconds)
    {
        double rand = Math.random();
        try {
        	Thread.sleep((long) (seconds * 1000 * rand) );
        }
        catch (InterruptedException ie) {
        	logger.error(ie.getMessage(), ie);
        }
    }
    	
}
