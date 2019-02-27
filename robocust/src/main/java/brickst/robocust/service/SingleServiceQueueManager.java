/**
 * @(#)SingleServiceQueueManager.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import org.apache.log4j.Logger;

/**
 * This class implements the ServiceQueueManager interface, providing management
 * of a single service queue.  (There is little involved in managing a single
 * service queue.
 */
public class SingleServiceQueueManager
	implements ServiceQueueManager
{
	static Logger logger = Logger.getLogger(SingleServiceQueueManager.class);

    ServiceQueue	sqSingle = null;

    /** @see com.kana.connect.lib.ServiceQueueManager#start */
    public void start(ServiceConfig config)
    {
        if (sqSingle == null) 
	        sqSingle = new ServiceQueue(config.getQueueLength());
    }
    	
    /** @see com.kana.connect.lib.ServiceQueueManager#reconfigure */
    public void reconfigure(ServiceConfig config)
    {
        if (sqSingle != null) {
            if (sqSingle.getQueueLength() != config.getQueueLength()) {
				if (logger.isDebugEnabled()) {	        	
					logger.debug(config.getName() + " changed: " 
			            + " current queueLength=" + sqSingle.getQueueLength() 
	    		        + " new queueLength=" + config.getQueueLength());
				}
		        sqSingle.setQueueLength(config.getQueueLength());
            }
        }
    }
    	
    	
    private boolean shutdownInProgress = false;
    public void shutdown()
    {
        shutdownInProgress = true;
    }

    /**
    * @see com.kana.connect.lib.ServiceQueueManager#tryDequeueServiceRequest
    */
    public boolean tryDequeueServiceRequest(ServiceThreadRequest request)
    {
		if (logger.isDebugEnabled()) logger.debug("dequeue " + request);

        ServiceQueueElement	sqe = sqSingle.tryDequeueSQE();
        if (sqe == null) {
	        if (shutdownInProgress) {
		        sqSingle = null;	// remove the queue
	        }
	        return(false);
        }

        request.serviceRequest = (ServiceRequest) sqe; // ClassCastException
        return(true);
    }

    /** @see com.kana.connect.lib.ServiceQueueManager#enqueueServiceRequest */
    public void enqueueServiceRequest(ServiceRequest request)
    {
		if (logger.isDebugEnabled()) logger.debug("enqueue " + request);

        if (shutdownInProgress == false)
	        sqSingle.enqueueSQE(request);	// May block.
    }

    /**
    * Returns true iff our ServiceQueue is full.
    * @return true iff our ServiceQueue is full.
    */
    public boolean isQueueFull()
    {
        return(sqSingle.isQueueFull());
    }

    //{{DECLARE_CONTROLS
    //}}
}
