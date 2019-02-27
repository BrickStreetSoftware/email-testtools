/**
 * @(#)ServiceThread.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import org.apache.log4j.Logger;

/**
 * ServiceThread extends Thread, which remembers the current "service"
 * (used also to describe daemons) and other displayable strings.
 */
public class ServiceThread extends Thread
{
	static Logger logger = Logger.getLogger(ServiceThread.class);

    /**
    * Every ServiceThread has a ServiceThreadRequest allocated to it.  When
    * we call CRMServices.getServiceThreadRequest(), it will modify our STR's
    * values.
    */
    private ServiceThreadRequest request = new ServiceThreadRequest();
    ServiceThreadRequest getServiceThreadRequest() { return(request); }

    static int getThreadCount() { return(countThreads); }

    private static int countThreads = 0;
    private static synchronized void adjustCount(int adj)
    {
        countThreads += adj;
        if (logger.isDebugEnabled()) {
			logger.debug("Service Threads # " + countThreads);
		}
    }

    /**
    * FUTURE: Decide what the ServiceThread constructor signature should look
    * like.  For now, we note the provided Service in our ServiceThreadRequest.
    * The ServiceList may choose to use our initial Service (observed as
    * initial because ServiceThreadRequest.sq == null) to influence our
    * choice of ServiceRequest's.
    */
    ServiceThread() { adjustCount(1); }

    //	ServiceThread(Service service) {
    //		request.service = service;
    //		updateCurrentService(service);
    //	}

    public static ServiceThread currentServiceThread()
    {
        return((ServiceThread) Thread.currentThread());	// ClassCastException?
    }

    public void run()
    {
        while (CRMServices.getServiceThreadRequest(this))
	    request.serviceHandler.handle(request.serviceRequest);
    }

    public void finalize()
        throws Throwable
    {
        adjustCount(-1);
        super.finalize();
    }
}
