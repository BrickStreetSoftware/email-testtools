/**
 * @(#)TrackerTesterServiceHandler.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;
import brickst.robocust.service.*;
import brickst.robocust.smtp.*;
import org.apache.log4j.Logger;
import java.util.ArrayList;

public class ReceiverTesterServiceHandler 
	implements ServiceHandler, VcReportProvider
{
	static Logger logger = Logger.getLogger(ReceiverTesterServiceHandler.class);

    /**
    * private static variable define statistics, configured in 
    * properties file
    */
    static ArrayList<ReceiverTester> handlers = null;
    private static final String NUMBER_OF_HANDLERS = "RV.NumberOfTesters";
    private static final String CLASS = "RV.testerClass";
    private static final String PERCENT = "RV.percent";

    public ReceiverTesterServiceHandler()
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
    		
    		handlers = new ArrayList<ReceiverTester>();
    		int size = config.getIntProperty(NUMBER_OF_HANDLERS);

    		ReceiverTester aTester;
    		for (int i = size; --i >= 0; ) {
    			String className = config.getProperty(CLASS + i);
    			try {
    				aTester = (ReceiverTester) Class.forName(className).newInstance();
    			}
    			catch (Exception x) {
    				logger.error("Error initializing Url Handler " + i +
    							", name: " + className + 
			                	"; using default handlers");
    				handlers = null ; //handlers[i] = new DefaultUrlHandler();
    				break;
    			}
    			aTester.setRandom( config.getDoubleProperty(PERCENT + i));
    			handlers.add(aTester);
    			logger.debug("handler " + i + ": " + aTester.toString());
    		}
        }
    }
	
    // choose handler using weighted random choice
    protected static ReceiverTester chooseHandler(ArrayList<ReceiverTester> hList)
    {
        double rand = Math.random();
        double beginRang = 0;
        double endRang = 0;

        for (int i = 0; i < hList.size(); i++) {
        	ReceiverTester rt = hList.get(i);
        	beginRang = endRang;
            endRang += rt.getRandom();

            if( rand > beginRang && rand <= endRang ) { 
            	return rt;            
            }
        }
        return null; 
    }
    
    /** @see com.kana.connect.lib.ServiceHandler#handle */
    // number of messages sent here defined in vcustomer.properties
    public void handle(ServiceRequest request)
    {
        ReceivedMailTestServiceRequest ttsr = (ReceivedMailTestServiceRequest) request;
        SmtpMessage msg = ttsr.getMessage();
        logger.debug(msg.getEnvelopeSender());
        
        ReceiverTester tester = chooseHandler(handlers);
        String handlerData = null;
        if (tester != null) {
        	logger.debug("Calling receiver tester " + tester.getClass().getName());
            try {
            	handlerData = tester.handle(msg);
            	VCLogger.getInstance().logMsgHandler(msg, tester.getHandlerName(), handlerData);
            }
            catch (Exception x) {
            	x.printStackTrace();
            }
        }
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName());
        for (int i=0; i<handlers.size(); i++)
        {
            ReceiverTester handler = handlers.get(i);
            sb.append("\r\n\t");
            sb.append(handler.getClass().getName());
        }
        return sb.toString();
    }
    
    public void getStatsReport(StringBuffer sb)
    {
        sb.append(this.getClass().getName());
        for (int i=0; i<handlers.size(); i++)
        {
            ReceiverTester handler = handlers.get(i);
            sb.append("\r\n\t");
            sb.append(handler.getClass().getName());
            sb.append(": ");
            handler.getStatsReport(sb);
        }
    }
}
