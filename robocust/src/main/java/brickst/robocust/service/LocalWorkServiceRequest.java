/**
 * @(#)LocalWorkServiceRequest.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;


public abstract class LocalWorkServiceRequest extends ServiceRequest
{
    /**
	* Initializes an object.  Sets up the SQE Multilist GUI to display
	* "Local Work" for the type and the classname for the data.  Caller can
	* override these values by calling ServiceQueueElement.setSQE{Data,Type}.
	*
	* @param	requestData	the description to use for the service request data.
	*						use.
	* @see ServiceRequest
	* @see ServiceQueueElement
	*/
    public LocalWorkServiceRequest(String requestData)
    {
	super("Local Work", requestData);
    }

    /**
	* Performs whatever work/action is associated with the handling of this
	* ServiceRequest.  Subclass must override and implement.  Called in the
	* context of the specified ServiceThread (provided for convenience).
	*
	* @param	current		The ServiceThread executing the method.  Provided
	*						for convenience (eg, calls to
	*						<code>current.updateStatus()<\code>).
	*/
    public abstract void performWork(ServiceThread current);
}
