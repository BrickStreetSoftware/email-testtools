/**
 * @(#)CRMServices.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import java.util.Hashtable;
import org.apache.log4j.Logger;

/**
 * This class provides the top level interface to services.  Note that the lock
 * here is the highest on in the system, so methods should be invoked without
 * holding locks.  All methods are static.
 *
 * For simplicity, a component can start providing a simple service with the
 * code specifying just the ServiceHandler:
 * <pre><code>
 *		ServiceHandler	handler = new LocalWorkServiceHandler());
 *		Service			service = CRMServices.createService("LocalWork");
 *		CRMServices.startProviding(service, handler);
 * </pre></code>
 * If a service requires a special ServiceProvider:
 * <pre><code>
 *		ServiceProvider provider = new MailSenderServiceProvider();
 *		Service			service = CRMServices.createService("MailSender");
 *		CRMServices.startProviding(service, provider);
 * </pre></code>
 * <br>
 * To start requesting a service using a default SingleServiceInstanceRequestor:
 * <pre><code>
 *		Service service = CRMServices.createService("LogFileTransfer");
 *		CRMServices.startRequesting(service);
 * </pre></code>
 * If a service requires a special ServiceRequestor:
 * <pre><code>
 *		ServiceRequestor requestor = new LogFileRemoveServiceRequestor();
 *		Service			 service = CRMServices.createService("LogFileRemove");
 *		CRMServices.startRequesting(service);
 * </pre></code>
 *
 * Two key lists are stored here:
 * <li> a list of all Services;
 * <li> a list of all ServiceProviders.
 *
 * CRMServices performs the following duties:
 * <li> Create (and register) services.
 * <li> Provide access to services via a string name.
 * <li> Start and stop services.
 * <li> Assign work (ServiceRequests) to ServiceThreads by fetching service
 *		requests from the most appropriate ServiceProvider.
 * <br>
 * Concurrency: The locking hierarchy for service related objects is, from
 * top-to-bottom:
 * <li> CRMServices -- this class's (static class) lock.
 * <li> listServices -- the list of all services.
 * <li> Service -- an individual service.
 * <li> serviceProviderList -- the pool of all service providers.
 * <li> ServiceProvider -- remembers service-related attributes (eg. max
 *		threads, priority) and the ServiceQueueManager.
 * <br>
 * Note that threads wait for work on serviceProviderList.
 * <p>
 * FUTURE: We may need a way to bind some set of threads to a ServiceProvider
 * to ensure some level of service.  Thus, we may want to have minimum thread
 * counts for providers.  Unfortunately, if we must support minimum thread
 * counts, we'd like to have those threads wait on their ServiceProvider,
 * which complicates our model.
 * <br>
 * FUTURE: Associate services with the component that starts them.
 * <br>
 * FUTURE: The client (requestor) of a service should not have to know what
 * ServiceRequestor must be used.  Perhaps class names for requestor and
 * (provider or handler) make sense.
 */
public final class CRMServices
{
	static Logger logger = Logger.getLogger(CRMServices.class);

    /** the list of all services running on this VM.  Used for lookup. */
    private static Hashtable<String,Service> listServices = new Hashtable<String,Service>();

    /**
    * the pool of all service providers running on this VM.  Searched to find
    * ServiceRequests to assign to ServiceThreads.  The list is sorted with
    * higher priority providers toward the head.  Initialized statically.
    */
    private static ServiceProviderList serviceProviderList = new ServiceProviderList();

    /**
    * Starts the services local to this VM.  The set of services started is
    * hard coded here.
    * <br>
    * Called by MonitorClient once the network configuration (and thus the
    * service configuration) is known.  MonitorClient may call us more than
    * once, so we protect against that.
    */
    public synchronized static void startLocalServices()
    {   
        if (fLocalServicesStarted)
	        return;

        fLocalServicesStarted = true;


        // Provide the Local Work service.
        startProviding(createService("LocalWork"),
				    LocalWorkServiceHandler.getInstance());

        // Start the client side of error escalation.
       // NodeConfig mc = NodeConfig.getLocalNode();
        //int vmid = NodeVM.getInstance().getVmId(); 
    //		if (fTracingEnabled)  {
    //			startRequesting(createService("Trace"),
    //							TraceServiceRequestor.getInstance());
    //		}

        // Enable periodic reporting of open file descriptors (InputStream and
        // OutputStream) to Debug.LB EVERY 5 MINUTES.
        //workItem wi = new WorkItem("com.kana.connect.server.service.CRMServices", 
	//						    "reportIOStreams");
       // Timed.getInstance().postWork(1000 * 300, wi, true);
    }
    private static boolean fLocalServicesStarted = false;

    private static boolean fTracingEnabled = false;

    /*
    * Enable or disable tracing (whether we will ever try to send TraceUpdate's
    * to the TraceUi) for this VM.  Must be called prior to startLocalServices.
    * <br>
    * FUTURE: This method is here only because there is no way to start
    * requesting the Trace service, then stop requesting it if we need to start
    * the Trace component (a provider).  Right now, there's no way to stop
    * requesting (at least, no way to do it synchronously).  We provide no
    * actual optimization by disabling tracing universally (though we could).
    *
    * @param enabled	true iff we should send TraceUpdate's.
    * @see CRMServices#startLocalServices
    public static void setTracingEnabled(boolean enabled)
    {
        fTracingEnabled = enabled;
    }
    */

    /** @return true if localServices  are started */
    public static boolean localServicesStarted()
    {
        return fLocalServicesStarted;
    }

    public static void reportIOStreams()
    {
    }

    /**
    * Returns the Service with the specified name; null if not found.
    *
    * @param	serviceName		the name of the service to locate.
    *
    * @return the Service with the specified name; null if not found.
    */
    public static Service lookupService(String serviceName)
    {
        synchronized (listServices)  {
			Service svc = listServices.get(serviceName);
			return svc;
		}
    }

    /**
    * Creates the Service with the specified name.
    *
    * @param	serviceName		the name of the service to locate.
    * @param    sc                      the configuration of the service
    *
    * @return   Service     
    */
    public static Service createService(String serviceName, ServiceConfig sc)
    {
        // For safetly, we lookup ServiceConfig before acquiring any locks.
        if (sc == null) {
            sc = ServiceConfig.lookupService(serviceName);
		}

        synchronized (listServices)  {
            // Handle duplicate calls to create service.
            Service service = lookupService(serviceName);
            if (service == null)
            {
                if (logger.isDebugEnabled()) {
					logger.debug("createService: creating service " + serviceName);
				}
                //service = sc.isPublicService()
				//	            ? new PublicService(serviceName, (int)sc.getId())
                service = new Service(serviceName);
                listServices.put(serviceName, service);
            }
            return(service);
        }
    }

    /**
    * Creates the Service with the specified name.
    *
    * @param	serviceName		the name of the service to locate.
    */
    public static Service createService(String serviceName)
    {
        return createService(serviceName, null);
    }

    /**
    * Starts providing the specified service using a default provider.
    *
    * @param	service	the service to start providing.
    * @param	handler	the service handle to use for processing requests.
    *
    * @see com.kana.connect.lib.CRMServices#startProviding(Service,
    *		ServiceProvider)
    */
    public static void startProviding(Service service, ServiceHandler handler)
    {
        //ServiceProvider provider = service instanceof PublicService
        //                        ? new PublicServiceProvider(handler)
        //                        : new ServiceProvider(handler);
        ServiceProvider provider = new ServiceProvider(handler);
        startProviding(service, provider);
    }

    /**
    * Starts providing the specified service.  Invokes the provider's
    * start method, which copies attributes from the ServiceConfig we supply
    * it.
    *
    * @param	service		the service to start providing.
    * @param	provider	the provider to use for the service.
    */
    public static void startProviding(Service service, ServiceProvider provider)
    {
        ServiceConfig   sc = ServiceConfig.lookupService(service.toString());

        synchronized (service) {
            // Permit multiple invocations.
            // FUTURE: We create this provider in our other constructor ;-(
            if (service.getServiceProvider() != null)
                return;
            
            //if (service instanceof PublicService  &&
            //    ((PublicService) service).getServiceRequestor() != null)
            //{
            //    stopRequesting(service);
                //Errors.println("CRMServices.startProviding(svc = " + service
                //			   + " -- disable requestor not supported yet!");
            //}

            service.start(provider);
            provider.start(sc);
            serviceProviderList.add(provider);
        }
    }

    /**
    * Stops providing the specified service.  Invokes the provider's
    * stop method and removes the provider from active duty.
    *
    * @param	service		the service to stostartp providing.
    */
    public static void stopProviding(Service service)
    {
        synchronized (service) {
	        service.shutdown();
	        // we cannot cleanup yet because the provider
	        // may be in the middle of something
	        // provider will call cleanup later at appropriate time
        }
    }

    public static void stopRequesting(Service service)
    {
        return ; //stopRequesting((PublicService) service);
    }

   /*  public static void stopRequesting(PublicService service)
    {
        // To run every component in one VM, we don't use requestors.
        if (!ServerConfig.fEnableMultipleVM)
	        return;

        synchronized (service) {
	        ServiceRequestor requestor = service.getServiceRequestor();
	        ((ServiceConnector)requestor).stop();
	        service.stop(requestor);
        }
    } */

    /**
    * called by services
    * when they are finally done with their cleanups
    * Concurrency: We must acquire no locks since we acquired from a
    * synchronized method in ServiceProvider.  REVIEW: unsynchronizing provider
    * where it calls us is not enough: the serviceProviderList lock is held as
    * well.  Hence cleanup doesn't work.
    */
    public static void cleanupService(Service service)
    {
        ServiceConfig   sc = ServiceConfig.lookupService(service.toString());
        ServiceProvider provider = service.getServiceProvider();

        service.cleanup();	// give a chance to service to do final cleanup

        synchronized (listServices)  {	// REVIEW: See concurrency.
	        if (provider != null)
		        serviceProviderList.remove(provider);
	        listServices.remove(service);
        }       
    }

    /**
    * Starts requesting the specified service.  Invokes the requestor's
    * start method, which copies attributes from the ServiceConfig we supply
    * it.
    *
    * @param	service	the service to start providing.
    */
    /* public static void startRequesting(PublicService service,
				    ServiceRequestor requestor)
    {
        // To run every component in one VM, we don't use requestors.
        if (!ServerConfig.fEnableMultipleVM)
	        return;

        ServiceConfig   sc = ServiceConfig.lookupService(service.toString());
        synchronized (service) {
	        // Permit multiple invocations.
	        // FUTURE: We create this provider in our other constructor ;-(
	        if (service.getServiceRequestor() != null  ||
		        service.getServiceProvider() != null)
		        return;

	service.start(requestor);
	((ServiceConnector)requestor).start();
	        // FUTURE: Do requestors care about sc.
	        // If not, can't Service start requestor by itself?
        }
    } */

    /**
    * Starts requesting the service, ensuring that it is public.
    *
    * @see com.kana.connect.lib.CRMServices#startRequesting
    */
    public static void startRequesting(Service service,
				    ServiceRequestor requestor)
    {
        return;
        //PublicService   pub = (PublicService) service;
        //startRequesting(pub, requestor);
    }

    /**
    * Starts requesting the service using the default
    * (SingleServiceInstanceRequestor).
    *
    * @see com.kana.connect.lib.CRMServices#startRequesting
    */
    public static void startRequesting(Service service)
    {
        startRequesting(service, null); //new SingleInstanceRequestor());
    }

    /**
    * Removes the service provider from the list of known service providers.
    * ServiceThreads will stop handling requests from this provider.
    *
    * @param provider	the service provider to remove.
    */
    /* UNUSED
    void remove(ServiceProvider provider)
    {
        serviceProviderList.remove(provider);
    }
    */

    /**
    * Locates a ServiceThreadRequest for calling ServiceThread to handle.  If
    * no requests are found, ServiceProviderList.fetchNextRequest() blocks
    * waiting for a call to notifyEnqueue() that decides to awaken the thread.
    * Called by the ServiceThread main execution loop.
    *
    * @param serviceThread	the calling thread, whose ServiceThreadRequest
    *						object we will modify and whose status we will
    *						update.
    */
    static boolean getServiceThreadRequest(ServiceThread serviceThread)
    {
        ServiceThreadRequest  request = serviceThread.getServiceThreadRequest();

        if (request.serviceProvider != null)
	        request.serviceProvider.
	                serviceRequestHandled(request.serviceRequest);
        request.clear();		// Avoid bugs by clearing all fields.

        // Fetch the next request.
        // serviceThread.updateStatusFast("Blocking ServiceRequest");
        if (!serviceProviderList.fetchNextRequest(serviceThread, request))
	        return(false);		// Thread should exit.

        return(true);
    }

    /** @see
    * com.kana.connect.server.service.ServiceProviderList#notifyEnqueue */
    public static void notifyEnqueue(ServiceProvider provider,
                                    ServiceRequest request,
                                    boolean providerWantsThread)
    {
        serviceProviderList.notifyEnqueue(provider, request, providerWantsThread);
    }
}
