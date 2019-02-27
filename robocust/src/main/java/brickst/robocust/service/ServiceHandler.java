/**
 * @(#)ServiceHandler.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

/**
 * The ServiceHandler interface implements a ServiceRequest processor.  Each
 * ServiceProvider is assigned a single ServiceHandler for processing
 * ServiceRequests made on the provider's service.
 */
public interface ServiceHandler
{
    /**
    * Handles the specified ServiceRequest.  Implementers may require that
    * the request have the type of one or more subclasses of ServiceRequest
    * dealt with by this handler (and thus this service); there is no way
    * right now to enforce the requirement.
    *
    * @param	request	the ServiceRequest requiring processing.
    */
    public abstract void handle(ServiceRequest request);
}
