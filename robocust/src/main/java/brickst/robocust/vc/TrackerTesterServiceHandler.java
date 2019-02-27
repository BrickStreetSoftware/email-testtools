/**
 * @(#)TrackerTesterServiceHandler.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;
import brickst.robocust.service.*;
import org.apache.log4j.Logger;

public class TrackerTesterServiceHandler
	implements ServiceHandler, VcReportProvider
{
	static Logger logger = Logger.getLogger(TrackerTesterServiceHandler.class);

    /**
    * private static variable define statistics, configured in 
    * properties file
    */
    static UrlHandler handlers[]= null;
    private static final String NUMBER_OF_HANDLERS = "TR.NumberOfTesters";
    private static final String CLASS = "TR.testerClass";
    private static final String URL = "TR.url";
    private static final String FOLLOW_REDIRECTS = "TR.FollowRedirects";
    private static final String PERCENT = "TR.clickPercent";

    public TrackerTesterServiceHandler()
    {
	init();
    }
    
    /**
    * init
    */
    private void init() 
    {
	if (handlers == null) {
	    SystemConfig config = SystemConfig.getInstance();
	    int size = config.getIntProperty(NUMBER_OF_HANDLERS);
	    handlers = new UrlHandler[size];
	    for (int i = size; --i >= 0; ) {
	        String className = config.getProperty(CLASS + i);
		try {
		    handlers[i] = (UrlHandler) Class.forName(className).newInstance();
		} catch (Exception x) {
		    logger.error("Error initializing Url Handler " + i +
		                ", name: " + className + 
		                "; using default handlers");
		    handlers[i] = new DefaultUrlHandler();
		}
	        handlers[i].setBaseUrl( config.getProperty(URL + i));
	        handlers[i].setFollowRedirects( config.getBooleanProperty(FOLLOW_REDIRECTS + i));
	        handlers[i].setRandom( config.getDoubleProperty(PERCENT + i));
		logger.debug("handler " + i);
		logger.debug(handlers[i].toString());
	    }
        }
    }
	
    // number of messages sent here are defined in vcustomer.properties
    /** @see com.kana.connect.lib.ServiceHandler#handle */
    public void handle(ServiceRequest request)
    {
	ReceivedMailTestServiceRequest	ttsr = (ReceivedMailTestServiceRequest) request;
	VCMessage msg = new VCMessage(ttsr.getMessage());
        
        String[] links = msg.getLinks();
        for (int i = 0; i < links.length; i++) {
            handleUrl(links[i]);
        }
    }
    
    private void handleUrl(String url) 
    {
	for (int i = 0; i < handlers.length; i++) {
	    if (handlers[i].handle(url)) {
	        break;
            }
	}
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName());
        for (int i=0; i<handlers.length; i++)
        {
            UrlHandler handler = handlers[i];
            sb.append("\r\n\t");
            sb.append(handler.getClass().getName());
            sb.append(" - [");
            sb.append(handler.getBaseUrl());
            sb.append("] ");
        }
        return sb.toString();
    }

    public void getStatsReport(StringBuffer sb)
    {
        sb.append(this.getClass().getName());
        for (int i=0; i<handlers.length; i++)
        {
            UrlHandler handler = handlers[i];
            sb.append("\r\n\t");
            sb.append(handler.getClass().getName());
            sb.append(" - [");
            sb.append(handler.getBaseUrl());
            sb.append("]: ");
            handler.getStatsReport(sb);
        }
    }
}
