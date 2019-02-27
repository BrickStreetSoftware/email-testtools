/**
 * @(#)ServiceQueueManager.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

/**
 * This interface can implemented by classes wishing to manage service queues
 * holding service requests.
 */
public interface ServiceQueueManager
{
    /**
    * Attempts to locate a ServiceRequest waiting for handling on a service
    * queue.  If found, it is dequeued.
    *
    * @param	request	the ServiceRequest (and related data) located
    *					(modified iff true is returned).
    *
    * @return	true, iff a ServiceRequest has been found, dequeued, and
    *			placed in the provided ServiceThreadRequest object.
    */
    boolean tryDequeueServiceRequest(ServiceThreadRequest request);

    /**
    * Adds the ServiceRequest to a ServiceQueue.
    *
    * @param	request		the ServiceRequest to be added.
    */
    void enqueueServiceRequest(ServiceRequest request);

    /**
    * Starts providing, fetching key configuration values from the
    * ServiceConfig (for now just the service queue length).  Must be called
    * before any other method.
    * 
    * @param	config	the ServiceConfig describing this service.
    */
    public void start(ServiceConfig config);
    	
    /** Reconfigure to new values in config (just the service queue length)
    * @param	config	the ServiceConfig describing this service.
    */
    public void reconfigure(ServiceConfig config);
    	
    /**
    * Stop providing. 
    * Called during shutdown of a component/service
    */
    public void shutdown();
	
}

// FUTURE: When multiple service queues were used, the ServiceThreadRequest
// stored the ServiceQueue from which a service request was dequeued.  The
// idea was to use the ServiceQueue to locate a cache of objects suitable for
// processing the request (eg, connections to the specific SMTP server the
// request called for).
//
// A better way now might be to leave space in the ServiceRequest and have the
// ServiceQueueManager leave a reference there during tryDequeue.  Then
// ServiceProvider.serviceRequestHandled() could invoke a similar method in
// ServiceQueueManager which would release the resource (whose reference can
// still be found in the ServiceRequest).
