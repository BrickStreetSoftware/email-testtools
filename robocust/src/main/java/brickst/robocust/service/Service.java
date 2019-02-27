/**
 * @(#)Service.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import org.apache.log4j.Logger;
import java.util.*;

/**
 * The Service class is the superclass for all services.  It remembers the
 * ServiceProvider, which places incoming ServiceRequest objects onto an
 * appropriate ServiceQueue and removes objects from these queues for
 * processing by ServiceThread's.
 * <br>
 * Service also stores many pieces of information for display in the service
 * MultiList.
 * <br>
 * It is the responsibility of the spawning component to stop the service when
 * appropriate.
 */
public class Service //extends ListItem
{
	static Logger logger = Logger.getLogger(Service.class);

    // Service priority codes
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL	= 2;
    public static final int PRIORITY_HIGH	= 3;

    /** the name of this service. */
    private String serviceName;

    /**
    * the ServiceProvider for this service.  The ServiceProvider manages all
    * aspects of incoming ServiceRequest handling and management.  Non-null
    * iff we are providing this service.
    * <br>
    * provider is set by the start method.
    *
    * @see com.kana.connect.services.Service#start().
    */
    private ServiceProvider provider = null;

    /**
    * Constructs a new service.
    *
    * @param	serviceName	the name of this service.
    */
    public Service(String serviceName)
    {
        this.serviceName = serviceName;
    }

    /**
    * Enable providing of this service.  Service must not already be providing.
    * <br>
    * FUTURE: We cannot synchronize here, even though we should.  It would
    * deadlock when we lock the ServiceList below.  Once GUI is removed,
    * synchronize this method.  REVIEW: What's this about????
    *
    * @param	provider	the ServiceProvider to use.
    */
    void start(ServiceProvider provider)
    {
        this.provider = provider;
        provider.setService(this);
    }

    private boolean shutdownInProgress = false;
    public void shutdown()
    {
        shutdownInProgress = true;
        provider.shutdown();
        this.provider = null;
        // FUTURE: Cleanup the gui list here
    }

    public void cleanup()
    {
        this.provider = null;
    }

    /**
    * Return the ServiceProvider associated with this service, or null if we
    * are not providing.
    *
    * @return	the ServiceProvider associated with this service; null if we
    *			are not providing.
    */
    public ServiceProvider getServiceProvider()
    {
        return(provider);
    }

    /**
    * Makes a request to the specified service.  Called by the "client".
    * The ServiceRequest should be of the type handled by the ServiceHandler
    * for this Service.  (However, since only components providing the
    * service define the ServiceHandler subclass responsible, we can make no
    * assertion here.)
    *
    * Note: This method must not be synchronized, since public services want
    * to block on the requestor only and since we never want to block with the
    * Service lock held.
    *
    * @param	request	the request to be made of this Service.
    *
    * @return	true iff the service was available for making the request.
    *
    * @see com.kana.connect.service.Service#enqueueRequest().
    */
    public boolean request(ServiceRequest request) 
    {
        if (shutdownInProgress == false)
	        return(enqueueRequest(request));

        return false;
    }

    /** Request a service and if the service is not available
    * don't wait for it */
    public boolean requestNoWait(ServiceRequest request)
    {
        // REVIEW1: Shouldn't this method actually attempt not to block on the
        // ServiceProvider?  It would seem important to clients.
        return request(request);
    }

    /**
    * Enqueues a request directly to the service's ServiceProvider.  
    * This method is called by ServiceReceiver's to enqueue a new request.
    * This differs from request(), the client interface, in that no requestor
    * shall be queried for a better target.
    *
    * @param	request	the ServiceRequest to be enqueued.
    *
    * @return	true iff the request could be enqueued to a ServiceQueue.
    *
    * @see com.kana.connect.service.Service#request().
    */
    public final boolean enqueueRequest(ServiceRequest request)
    {
        // To do this unsynchronized, we store the provider value in a local
        // variable to ensure it does not change while we're using it.
        ServiceProvider	p = getServiceProvider();
        if (logger.isDebugEnabled()) {
        	logger.debug("enqueueRequest in Service, provider: " + p);
        }
        if (p != null)  {
            boolean	providerWantsThread = p.enqueueServiceRequest(request);

            // We must advise CRMServices that we have enqueued an object.
            CRMServices.notifyEnqueue(provider, request, providerWantsThread);

            return(true);
        } else {
            return(false);
        }
    }

    /**
    * Determine whether this service is providing locally.
    * @return true iff this service is providing.
    */
    public final boolean isALocalProvider()
    {
        return(provider != null);
    }

    public String toString() { return(serviceName); }

    public String getRunStatus()
    {
        return(provider != null ?  "Providing" : "Unavailable");
    }
    	
    /** Reconfigure to the new service paramaters */
    public void reconfigure(ServiceConfig sc)
    {
        getServiceProvider().reconfigure(sc);
    }
}
