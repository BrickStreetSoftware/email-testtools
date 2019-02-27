/**
 * @(#)ServiceProviderList.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import brickst.robocust.lib.*;
import org.apache.log4j.Logger;

/**
 * The ServiceProvider class is the superclass for all providers.  We remember:
 * <li> the ServiceHandler used for processing requests on this provider.
 * <li> the ServiceQueueManager used to manage our service queue(s).
 *
 * The service provider updates numbers in the Service GUI: requests in queue;
 * threads processing requests.
 *
 * Note that the ServiceProvider is not usable until setService() has been
 * called.
 */
public class ServiceProvider //extends ListItem
{
	static Logger logger = Logger.getLogger(ServiceProvider.class);
	
	/** the priority of this ServiceProvider relative to others. */
    private int priority;

    /** the number of ServiceThreads currently handling requests for this
    * ServiceProvider. */
    private SimpleCounter countThreadsCur = new SimpleCounter();

    /** the number of ServiceRequests currently in our queue. */
    private SimpleCounter countRequestsInQueue = new SimpleCounter();

    /**
    * the total number of ServiceRequests handled via this provider.
    * The counter is incremented tryDequeueServiceRequest().
    */
    private SimpleCounter countRequestsHandled = new SimpleCounter();
    	
    /** the maximum number of ServiceThreads allowed concurrently to handle
    * requests for this ServiceProvider.  Initialized from the ServiceConfig in
    * start(). */
    private int threadCountMax = 0;

    /**
    * the ServiceHandler responsible for processing ServiceRequests.
    * Recorded in the ServiceThreadRequest object upon a successful dequeue;
    * caller will invoke the handler when we return from the dequeue.
    * Initialized in constructor.
    */
    private ServiceHandler handler;

    /** the service queue manager responsible for enqueueing and dequeueing
    * ServiceRequests. */
    private ServiceQueueManager serviceQueueManager;

    /**
    * the service we provide.  Note that the Service lock is above ours on
    * the locking hierarchy.  We don't access it, only share it with others.
    * Eg, when populating a ServiceThreadRequest during dequeue, we stamp the
    * Service value for use by the handling thread.
    */
    private Service	service = null;
    	
    /**
    * Constructs a new ServiceProvider using a SingleServiceQueueManager.
    *
    * @param	handler     the service handler to invoke on requests.
    */
    public ServiceProvider(ServiceHandler handler)
    {
        this(handler, new SingleServiceQueueManager());
    }

    /**
    * Constructs a new ServiceProvider.
    *
    * @param	handler				the service handler to invoke on requests.
    * @param	serviceQueueManager	the service queue manager.
    */
    public ServiceProvider(ServiceHandler handler,
		          ServiceQueueManager serviceQueueManager)
    {
        this.handler = handler;
        this.serviceQueueManager = serviceQueueManager;
    }

    /**
    * Sets our service.  Called by Service.start(this) so we can
    * stamp each ServiceThreadRequest with the service (for Debugging).
    *
    * @param service	the service we provide.
    */
    synchronized void setService(Service service)
    {
        this.service = service;
    }

    Service getService() { return(service); }

    /**
    * Return the relative priority of this service.
    *
    * NOTE: this method must not be synchronized.
    *
    * @return the relative priority of this service.
    */
    int getPriority() { return(priority); }

    /**
    * Starts providing, fetching key configuration values from the
    * ServiceConfig.  These include max threads, max objects, etc.
    * 
    * @param	config	the ServiceConfig describing this service/provider.
    */
    public void start(ServiceConfig config)
    {
        threadCountMax = config.getMaxThreads();
        priority = convertToThreadPriority(config.getPriority());
        serviceQueueManager.start(config);
    }
    	
    /**
    * boolean to indicate that shutdown of this provider is in progress
    */
    private boolean shutdownInProgress = false;
    public void shutdown()
    {
        shutdownInProgress = true;
        serviceQueueManager.shutdown();
    }

    public ServiceQueueManager getServiceQueueManager()
    {
        return(serviceQueueManager);
    }

    private boolean wantMoreThreads()
    {
        return(countThreadsCur.getValue() < threadCountMax);
    }

    /**
    * Enqueues a request to the ServiceProvider's ServiceQueueManager.
    * Called by Service.
    *
    * @param	request	the ServiceRequest to be enqueued.
    *
    * @return	true if the provider would like more threads to handle requests.
    */
    final boolean enqueueServiceRequest(ServiceRequest request)
    {
        if (shutdownInProgress)
            return(false);

        serviceQueueManager.enqueueServiceRequest(request);	// may block

        // REVIEW: Because enqueueServiceRequest does not drop our lock, we
        // cannot hold it when calling it.  This creates a RACE in which the
        // someone may dequeue the object and decrement countRequestsInQueue
        // before we increment it, potentially causing the count to become
        // negative!
        synchronized (this) {
            countRequestsInQueue.increment();
            return(wantMoreThreads());
        }
    }

    /**
    * Attempts to locate a ServiceRequest waiting for handling in our service
    * queues.  Invokes the ServiceQueueManager.  Records useful data in the
    * ServiceThreadRequest object.
    * <br>
    * This method decrements countRequestsInQueue and increments
    * countThreadsCur.
    *
    * @param	request	the ServiceRequest (and related data) located
    *					(modified iff true is returned).
    * @return	true, iff a ServiceRequest has been found, dequeued, and
    *			placed in the provided ServiceThreadRequest object.
    */
    final synchronized boolean tryDequeueServiceRequest(
        ServiceThreadRequest request)
    {
        // Don't allow more than 'threadCountMax' threads simultaneously to
        // handle service requests.
        if (!wantMoreThreads())
	        return(false);

        // REVIEW: Is it true that tryDequeueServiceRequest returns false iff
        // countRequestsInQueue == 0 ???  If so, don't ask the queue manager.
        // I've introduced an assertion to test the hypothesis.  See the RACE in
        // enqueueServiceRequest for why it may fail.
        // boolean fHadMoreRequests = countRequestsInQueue.getValue() > 0;
        if (serviceQueueManager.tryDequeueServiceRequest(request))  {
        	if (logger.isDebugEnabled()) {
        		logger.debug("Dequeued (request = " + request.serviceRequest + ")");
        		logger.debug("service " + service);
        		logger.debug("handler " + handler);
        		logger.debug("provider " + this);
        	}
	        countRequestsInQueue.decrement();
	        countThreadsCur.increment();		// Thread joins this provider.

	        request.service = service;
	        request.serviceHandler = handler;
	        request.serviceProvider = this;
	        return(true);
        } else {	// nothing more to process in the queue
	        // Debug.assert(!fHadMoreRequests);
	        if (shutdownInProgress) {
		        CRMServices.cleanupService(service); // BUG: See CRMServices
		        serviceQueueManager = null;
		        handler = null;
	        }
	        return(false);
        }
    }

    /**
    * Called by CRMServices when a request has been handled.  We remove the
    * thread from our count of those working on requests from this provider.
    */
    synchronized void serviceRequestHandled(ServiceRequest request)
    {
        countThreadsCur.decrement();		// Thread leaves this provider.
        countRequestsHandled.increment();
    }

    /**
    * @return the maximum number of ServiceThreads allowed concurrently to
    * handle requests for this provider.
    */
    int getMaxServiceThreads()
    {
        return(threadCountMax);
    }

    /** Reconfigure to new service parameters */
    public void reconfigure(ServiceConfig config)
    {
        if (threadCountMax != config.getMaxThreads()) {
        	if (logger.isDebugEnabled()) {
        		logger.debug(config.getName() + " changed: " 
	                        + " current threadCount=" + threadCountMax 
	                        + " new threadCount= " + config.getMaxThreads());
        	}
            threadCountMax = config.getMaxThreads();
        	                           
        }
        if (priority != convertToThreadPriority(config.getPriority())) {
        	if (logger.isDebugEnabled()) {
        		logger.debug(config.getName() + " changed: " 
		                + " current priority=" + priority 
		                + " new priority= " 
		                    + convertToThreadPriority(config.getPriority()));
        	}
            priority = config.getPriority();
        }
        		    
            serviceQueueManager.reconfigure(config);
    }
    	
    public static int convertToThreadPriority(int priority)
    {
        if (priority == Service.PRIORITY_HIGH) 
            return Thread.MAX_PRIORITY;
        else if (priority == Service.PRIORITY_NORMAL)
            return Thread.NORM_PRIORITY;
        else
            return Thread.MIN_PRIORITY;
    }
}
