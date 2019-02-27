/**
 * @(#)ServiceConfig.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import java.util.ArrayList;

/**
 * ServiceConfig describes a service and its properties
 * A service is described by its name, and other characteristics
 */
public class ServiceConfig
    implements java.io.Serializable
{
    private String name = null;
    private int maxThrCount = 0;
    private int queueLength = 0;
    private int priority = PRIORITY_NORMAL;
    private int maxConnectionCount = 0;
    private boolean isAPublicService;
    private int provideTo;
    private boolean editable = false;

    private static final boolean A_PUBLIC_SERVICE = true;
    private static final boolean IS_EDITABLE_FROM_GUI = true;

    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL	= 2;
    public static final int PRIORITY_HIGH	= 3;
    
    //the list of ServiceConfig within this vm
    private static ArrayList<ServiceConfig> list = new ArrayList<ServiceConfig>();
        
    public static final int ALL = 0;
    /*  public static final int INSIDE_FIREWALL = 1;
	public static final int OUTSIDE_FIREWALL = 2;
	public static final int NODE = 3;
    */
    
    public ServiceConfig(String svcName, 
			int svcMaxThrCount, int svcQueueLength,
			int priority, int svcMaxConnCount,
			boolean isAPublicService,
			boolean editable)
    {
        this(svcName, svcMaxThrCount, svcQueueLength,
		        priority, svcMaxConnCount, isAPublicService, ALL, editable);
    }

    /** Create a ServiceConfig given its name, maxThreadCount,
    *  maxQueueLength, priority, maxConnectionCount, package info */
    public ServiceConfig(String svcName, 
			int svcMaxThrCount, int svcQueueLength,
			int priority, int svcMaxConnCount,
			boolean isAPublicService,
			int provideTo,
			boolean editable)
    {
        this.name = svcName;
        maxThrCount = svcMaxThrCount;
        maxConnectionCount = svcMaxConnCount;
        queueLength = svcQueueLength;
        this.priority = priority;
        this.isAPublicService = isAPublicService;
        this.provideTo = provideTo;
        this.editable = editable;
    }

    /**
     * Add the service config to the list
     *
     * @param sConfig   a service config
     */
    public static void addServiceConfig(ServiceConfig sc) 
    {
        ServiceConfig sc_current = lookupService(sc.getName());
        if (sc_current == null)
            list.add(sc);
    }
    
    /** Lookup a service given its name in a services list */
    public static ServiceConfig lookupService(ArrayList<ServiceConfig> list, String name)
    {
        for (int i=0; i<list.size(); i++) {
            ServiceConfig sc = (ServiceConfig)list.get(i);
            if (sc.getName().equals(name))
                return sc;
        }
        return null;
    }

    /** Lookup a service given its name in the current deployment */
    public static ServiceConfig lookupService(String name)
    {
	return lookupService(list, name);
    }

    public final String toString() { return name; }

    /** return the service name */
    public final String getName() { return(name); }

    /** return the service priority */
    public final int getPriority() { return priority; }

    public final void setPriority(int pri)
    {
        priority = pri;
    }

    /** return the maximum number of threads service can use */
    public final int getMaxThreads() { return(maxThrCount); }
    public final void setMaxThreads(int mx)
    {
        maxThrCount = mx;
    }

    /** return the maximum number of connections service can use */
    public final int getMaxConnections() { return maxConnectionCount; }
    public final void setMaxConnections(int mx)
    {
	maxConnectionCount = mx;
    }

    /** return the length to use for service queues. */
    public final int getQueueLength() { return(queueLength); }
    public final void setQueueLength(int mx)
    {
        queueLength = mx;
    }

    /** return true if this is a public service */
    public final boolean isPublicService() { return(isAPublicService);  }

    public final int provideTo()
    {
        return provideTo;
    }

    /** return true if this service parameters are editable */
    public final boolean isEditable() {return editable;}

}
