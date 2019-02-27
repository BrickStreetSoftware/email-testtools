package brickst.robocust.vc;

import brickst.robocust.lib.*;
import brickst.robocust.connection.*;
import brickst.robocust.service.*;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class VC implements ServerSocketProcessor
{
	static Logger logger = Logger.getLogger(VC.class);
    private static ArrayList<VcService> testServices;
    private static ArrayList<ServiceHandler> handlers = new ArrayList<ServiceHandler>();
    
    private VC() {}
    
    static public void main(String args[])
    {
    		
    	// initialize SystemConfig
    	String configFile = "robocust.properties";
    	if (args.length > 0) {
    		// override default file name
    		configFile = args[0];    		
    	}
    	
        init(configFile);
        
        SystemConfig sc = SystemConfig.getInstance();
        
        int vcSmtpPort =  sc.getIntProperty(SystemConfig.SMTP_PORT); 
        
        new ServerSocketListener((new VC()),		// ServerSocketProcessor
				"VirtualCustomer",
				vcSmtpPort).start(); // Starts accepting connections.
         			
        // Start VC DNS server
        VcDnsdServer vcdns = null;
        try {
        	vcdns = new VcDnsdServer(sc);
        	vcdns.start();
        }
        catch (SocketException sx) {
        	throw new RuntimeException(sx);
        }

        // Print out statistics every STATS_PERIODICITY_SECONDS
        int sleepTimeMs = SystemConfig.getInstance().getIntProperty(SystemConfig.STATS_PERIODICITY_SECONDS) * 1000;
        boolean displayWhenChanged = SystemConfig.getInstance().getBooleanProperty(SystemConfig.STATS_DISPLAY_ONLY_WHEN_CHANGED);
        String previousCounters = null;
        while (true)
        {
			try {
	            Thread.sleep(sleepTimeMs);
			}	
			catch (InterruptedException x) {}

            StringBuffer sb = new StringBuffer("Counters: throughput(s), throughputValue " + 
                    ", averageLatency, maxLatency, value \r\n");
            sb.append("Messages received: ");
            sb.append(VCLogger.getInstance().getMsgReceived());
            for (int i=0; i<handlers.size(); i++)
            {
                VcReportProvider handler = (VcReportProvider) handlers.get(i);
                sb.append("\r\n");
                handler.getStatsReport(sb);
            }
            String currentCounters = sb.toString();
            // Print only when changed if so desired
            // (Print when STATS_DISPLAY_ONLY_WHEN_CHANGED is false or when they have changed.)
            if (!displayWhenChanged || !currentCounters.equals(previousCounters))
            {
                logger.info(currentCounters);
                previousCounters = currentCounters;
            }
        }
    }

    /**
     * Init the vm. Start services specified in VC configuration
     */
    private static void init(String configFile) 
    {
    	try {
    		SystemConfig.init(configFile);
    	}
    	catch (IOException x) {
    		x.printStackTrace();
    		throw new RuntimeException(x);
    	}
    	
	/*
        try {
			DOMConfigurator.configure("/log4j.xml");
        } catch (Exception ex) {
            System.out.print(ex.toString());
        }
	*/
	
        /*
         * Initialize Subsystems
         */
    	VcReceiver.init();
    	
    	/*
    	 * Configure Services
    	 */
    	SystemConfig sc = SystemConfig.getInstance();      	
        int noOfService = sc.getIntProperty(SystemConfig.NUMBER_OF_TEST_SERVICE);
        
        testServices = new ArrayList<VcService>();
        
        for (int i = 0; i < noOfService; i++) {
            String name = sc.getProperty(SystemConfig.VC_SERVICE + i + "." + SystemConfig.NAME);
            String config = sc.getProperty(SystemConfig.VC_SERVICE + i + "." + SystemConfig.CONFIG);
            StringTokenizer st = new StringTokenizer(config, ",");
            int threads = Integer.parseInt(st.nextToken());
            int connections = Integer.parseInt(st.nextToken());
            int queue = Integer.parseInt(st.nextToken());
            int priorityCode = Integer.parseInt(st.nextToken());
            ServiceConfig svc = new ServiceConfig(name, threads, queue, 
                                                priorityCode, connections, true, true);
            ServiceConfig.addServiceConfig(svc);
        
            String handlerName = sc.getProperty(SystemConfig.VC_SERVICE + i + "." + SystemConfig.SERVICE_HANDLER);
            Service service = CRMServices.createService(name);
            ServiceHandler handler = getHandler(handlerName);
            CRMServices.startProviding(service, handler);
            handlers.add(handler);
            
            double rand = sc.getDoubleProperty(SystemConfig.VC_SERVICE + i + "." + SystemConfig.PERCENT);
            VcService testService = new VcService(service, rand);
            
            logger.debug("add VcService " + testService);
            testServices.add(testService);
        }
        
        //start reliableEmailAgent
    	//ReliableEmailAgent.getInstance(VCConfig.getInstance());
    }
    
    private static ServiceHandler getHandler(String className)
    {
        try {
	    ServiceHandler handler = (ServiceHandler) Class.forName(className).newInstance();
	    return handler;
	}  catch (Exception x) {
	    logger.error("Error initializing inbound email handler " + className );
	    logger.error(x);
	}
        return null;
    }
    
    protected static ArrayList<VcService> getTestServices() { return testServices; }
    
    /**
     * This function will pick up a "random" class out of a list of them. <br>
     * Note: the summary of those "random" class should be <= 1.
     *
     * @param randoms   a list of "random" class
     * @return IRandom  a "random" class that has been chosen, null if nothing got picked up
     */
    protected static VcService chooseARandom(ArrayList<VcService> services)
    {
        double rand = Math.random();
        double beginRang = 0;
        double endRang = 0;

        for (int i = 0; i < services.size(); i++) {
        	VcService vs = services.get(i);
        	beginRang = endRang;
            endRang += vs.getRandom();

            if( rand > beginRang && rand <= endRang ) { 
            	return vs;            
            }
        }
        return null; 
    }
    
    /**
    * Creates a new SmtpServerRequest and sends it to our service.
    *
    * @see
    * com.kana.connect.server.lib.ServerSocketProcessor#handleNewConnection
    */
    public void handleNewConnection(Socket s)
    {
        new VcReceiver(s).start();
        
	//Debug.VC.println("Connection: " + s.getInetAddress().toString());
    }
}
