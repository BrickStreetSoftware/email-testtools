/**
 * @(#)ServiceQueueElement.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

/**
 * The ServiceQueueElement class records data about a service queue element that
 * can be used for display by another list.  It does not send trace updates
 * itself.
 *
 * Two descriptive strings are stored by this class.  These strings are provided
 * for programmers, so they can see descriptions of the elements moving through
 * the system.  They are:
 * <li> The "element type" -- generally shorthand for the class of a
 *      ServiceQueueElement's most derived subclass.  E.g., "EMail" or
 *		"Instance".
 * <li> The "element data" -- a string which helps better identify the
 *		individual element from others that share its type.  E.g., for "EMail",
 *		the data might be the target MX Host or the recipient; for "Instance",
 *		the data might be the interaction name (e.g., "Weekly Newsletter").
 * <br>
 * FUTURE: Automate the sending of trace updates when the element is changed.
 */
public class ServiceQueueElement //extends ListItem
	implements java.io.Serializable
{
    /**
     * Constructs a new ServiceQueueElement.  Parameters default to empty
     * strings.
     *
     * @param   elementType   a string specifying the element type.
     * @param   elementData   a string specifying the element data.
     */
    public ServiceQueueElement()
    {
        this("");
    }
    public ServiceQueueElement(String elementType)
    {
        this(elementType, "");
    }
    public ServiceQueueElement(String elementType, String elementData)
    {
        setElementType(elementType);
        setElementData(elementData);
    }

    /** the element type. */
    private String elementType;

    /** the element data. */
    private String elementData;

    /**
    * Returns a string specifying the element type.
    *
    * @returns a string specifying the element type.
    */
    public String getElementType()
    {
        return(elementType);
    }

    /**
    * Sets the element type.
    */
    public void setElementType(String elementType)
    {
        this.elementType = elementType;
    }

    /**
    * Returns a string specifying the element data.
    *
    * @returns a string specifying the element data.
    */
    public String getElementData()
    {
        return(elementData);
    }
     
    /**
    * Sets the element data.
    */
    public void setElementData(String elementData)
    {
        this.elementData = elementData;
    }
}
