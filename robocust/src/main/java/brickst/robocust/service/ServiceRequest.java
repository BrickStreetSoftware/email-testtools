/**
 * @(#)ServiceRequest.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

/**
 * ServiceRequest is the superclass for any Service-specific object which may
 * be transmitted over a ServiceConnection (and whose receipt will be handled
 * by the service's corresponding ServiceHandler).
 *
 * ServiceRequest uses its parent ServiceQueueElement to handle descriptive
 * data -- "element type" and "element data".  Wrapper methods are provided to
 * set the info as "service request type" and "service request data"; they are
 * public to permit access outside the lib package, while the
 * ServiceQueueElement methods they replace are deprecated to discourage
 * usage from this class.
 */
public class ServiceRequest extends ServiceQueueElement
	implements java.io.Serializable
{  
	private static final long serialVersionUID = 8758430726282746464L;
	private int serviceInstance = 1;		// 1 is default
    
    /**
    * Constructs a new ServiceRequest, optionally specifying one or two
    * strings describing the request.
    *
    * @param	requestType	the service request type, passed to parent class.
    * @param	requestData	the service request data, passed to parent class.
    * @see com.kana.connect.lib.ServiceQueueElement
    */
    public ServiceRequest() { }
    public ServiceRequest(String serviceRequestType)
    {
        super(serviceRequestType);
    }

    public ServiceRequest(String serviceRequestType, String serviceRequestData)
    {
        super(serviceRequestType, serviceRequestData);
    }

    /**
    * Returns a string containing the serviceRequestType and serviceRequestData
    * objects for use outside the ServiceQueueElement MultiList.  Entities that
    * handle ServiceRequest's use this to describe their activity in a GUI.
    *
    * @return	a String identifying this ServiceRequest.
    */
    public final String getServiceRequestQuickDescription()
    {
        String	serviceRequestType = getElementType();
        String	serviceRequestData = getElementData();

        if (serviceRequestType == null)
	        return(serviceRequestData);
        if (serviceRequestData == null)
	        return(serviceRequestType);
        return(serviceRequestType + ": " + serviceRequestData);
    }

    public String toString()
    {
        return("" + super.toString() +
		    '(' + getServiceRequestQuickDescription() + ')');
    }

    /**
    * Returns a string specifying the service request type.
    *
    * @returns a string specifying the service request type.
    */
    public String getServiceRequestType()
    {
        return(getElementType());
    }

    /**
    * Sets the service request type.
    */
    public void setServiceRequestType(String serviceRequestType)
    {
        super.setElementType(serviceRequestType);
    }

    /**
    * Returns a string specifying the service request data.
    *
    * @returns a string specifying the service request data.
    */
    public String getServiceRequestData()
    {
        return(getElementData());
    }

    /**
    * Sets the service request data.
    */
    public void setServiceRequestData(String serviceRequestData)
    {
        super.setElementData(serviceRequestData);
    }

    /**
    * @see com.kana.connect.lib.ServiceQueueElement#getElementType()
    * @deprecated
    */
    public String getElementType()
    {
        return(super.getElementType());
    }

    /**
    * @see com.kana.connect.lib.ServiceQueueElement#setElementType()
    * @deprecated
    */
    public void setElementType(String elementType)
    {
        super.setElementType(elementType);
    }

    /**
    * @see com.kana.connect.lib.ServiceQueueElement#getElementData()
    * @deprecated
    */
    public String getElementData()
    {
        return(super.getElementData());
    }

    /**
    * @see com.kana.connect.lib.ServiceQueueElement#setElementData()
    * @deprecated
    */
    public void setElementData(String elementData)
    {
        super.setElementData(elementData);
    }

    /**
    * Targets a specific instance of a service. 
    */
    public final void setServiceInstance(int inst)
    {
        serviceInstance = inst;
    }

    public final int getServiceInstance()
    {
        return serviceInstance;
    }
}
