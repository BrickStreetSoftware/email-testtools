/**
 * @(#)ServiceRequestor.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

/**
 * This interface class  represents the request side of service request transfer.
 * Classes implementing it establish connections to servers (ServiceInstance's)
 * providing a service.
 * <br>
 * Unlike the ServiceReceiver which has a thread associated with it, a requestor
 * is passive.  When we support callbacks, this may not be true;
 * ServiceRequestor and ServiceInstance will likely change to support this.
 * <br>
 * @see com.kana.connect.lib.SingleInstanceRequestor
 * @see com.kana.connect.lib.MultipleInstanceRequestor
 */
public interface ServiceRequestor 
{
    public void start();
    public void reconfigure(ServiceConfig sc);
    
    /**
    * Tries to send the specified service request to an available service
    * provider.
    *
    * @param	request	the service request to send.
    * @return	true if the request was sent successfully; false if an error
    *			occurred.
    */
    public boolean request(ServiceRequest request);
    	
    /** Try to send a request and if a provider is not available yet
    * or if we cannot get connections to the provider return immediately */
    public boolean requestNoWait(ServiceRequest request);
}
