package brickst.robocust.connection;

import org.apache.log4j.Logger;
import java.net.*;
import java.io.IOException;

/**
 * The ServerSocketListener class accepts connections from the specified
 * port and invokes its abstract processRequest() method with each.
 *
 * FUTURE: Separate listening and accepting into two classes, thus allowing
 * SocketAcceptor to extend ServerSocketListener while Tracker extends
 * ServerSocketAcceptor (which extends ServerSocketListener).
 */
public class ServerSocketListener extends Thread
{
	static Logger logger = Logger.getLogger(ServerSocketListener.class);

    private int port;

    private String name;

    private ServerSocketProcessor processor;
    	
    //private ErrorMonitor errorMonitor;

    public ServerSocketListener(ServerSocketProcessor processor,
                                String name, int port)
    {
        this.name = name;
        this.port = port;
        this.processor = processor;
    		
        //errorMonitor = new ErrorMonitor();
        //errorMonitor.addEscalation(30, ErrorTypes.IO_FAILURE);        	
    }
    public ServerSocketListener(String name, int port)
    { this(null, name, port); }
    	
    public void setProcessor(ServerSocketProcessor processor)
    {
        //Debug.assert(this.processor == null);	// Sanity
        //Debug.assert(processor != null);		// Sanity
        this.processor = processor;
    }

    public void run()
    {
        while (true)  {
            ServerSocket s = getServerSocket(port);
            logger.debug(name + " is listening on port " + port);
            processRequests(s);
        }
    }

    private void processRequests(ServerSocket s)
    {
    	try {
    		while (true) {
    			try {
    				Socket newsock = s.accept();
    				logger.info("accept: " + newsock.getInetAddress().getHostAddress() + ":" + newsock.getPort());
    				processor.handleNewConnection(newsock);
    				//errorMonitor.succeeded();
    			} catch (java.io.IOException ex) {
    				// This exception will only be thrown if s gets closed.
    				// We never close s, so we will never get this exception.
    				//Errors.println(ex, "Unexpected exception during accept() for " +
    				//				            name + " listening on port " + port);
    				//errorMonitor.failed();
    				logger.error("processRequests Error", ex);
    				return;
    			}
    		}
    	}
    	finally {
        	if (s != null) {
        		try {
        			s.close();
        		}
        		catch (IOException iox) {}            		
        	}
    	}
    }
    
    public static ServerSocket getServerSocket(int port)
    {
        int cFailures = 0;
        while (true) {
            try {
                return(new ServerSocket(port));
            } catch (IOException ex) { // BindException
                logger.debug(null, ex);
                //new CRMError(ErrorTypes.PORT_BIND_FAILURE,
		//		            "port = " + port, ex).escalateSleep(++cFailures);
            }
        }
    }
}
