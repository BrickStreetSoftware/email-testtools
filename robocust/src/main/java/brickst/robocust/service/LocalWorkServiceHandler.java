/**
 * @(#)LocalWorkServiceHandler.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

public class LocalWorkServiceHandler
	implements ServiceHandler
{
    /** the singleton instance of this class. */
    private static LocalWorkServiceHandler instance = null;

    /** Returns the singleton instance of this class.
    * @return the singleton instance of this class. */
    public static synchronized LocalWorkServiceHandler getInstance()
    {
        if (instance == null)
	        instance = new LocalWorkServiceHandler();
        return(instance);
    }

    /**
    * Constructs the LocalWorkServiceHandler object.
    */
    private LocalWorkServiceHandler() { }

    /** @see com.kana.connect.lib.ServiceHandler#handle */
    public void handle(ServiceRequest request)
    {
        LocalWorkServiceRequest	work = (LocalWorkServiceRequest) request;
        work.performWork(ServiceThread.currentServiceThread());
    }
}
