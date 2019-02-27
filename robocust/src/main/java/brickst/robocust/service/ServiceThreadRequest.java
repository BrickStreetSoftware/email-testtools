/**
 * @(#)ServiceQueueManager.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;


/**
 * This class provides everything a generic ServiceThread requires to
 * handle an arbitrary service request.  It is passed through the Service
 * Architecture while dequeuing a ServiceRequest to record data associated
 * with the request.
 *
 * @see com.kana.connect.lib.ServiceThread
 */
public class ServiceThreadRequest
{
    /** the service request to process. */
    public ServiceRequest	serviceRequest = null;

    /** the service handler which the ServiceThread must run to process this
    * request. */
    public ServiceHandler serviceHandler = null;

    /** the service from which the request was dequeued. */
    public Service service = null;

    /** the service provider from which the request was dequeued.  The provider
    * is notified XXXXX FUTURE? */
    public ServiceProvider serviceProvider = null;

    public void clear()
    {
        serviceRequest = null;
        serviceHandler = null;
        service = null;
        serviceProvider = null;
    }
}
