/**
 * @(#)ForwardingServiceHandler.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import org.apache.log4j.Logger;

/**
 * This class provides a ServiceHandler that forwards all ServiceRequests to
 * another Service (via Service.request()).<br>
 *
 * This can be used, eg, to prevent a main thread from blocking on a connection
 * in the given service when other connections may be free.  CM's execute
 * threads delegate the actual sending of an EMail request to a private service
 * that employs a ForwardingServiceHandler.
 */
public class ForwardingServiceHandler
    implements ServiceHandler
{
	static Logger logger = Logger.getLogger(ForwardingServiceHandler.class);

    /** the service to which we forward all requests. */
    private Service service;

    public ForwardingServiceHandler(Service service) { this.service = service; }

    /** @see com.kana.connect.server.service.ServiceHandler#handle */
    public void handle(ServiceRequest request)
    {
        if (!service.request(request)) {
		    if (logger.isDebugEnabled()) {
				logger.debug(service.toString() + " service request failed.");
			}
		}
    }
}
