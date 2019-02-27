/*
 * @(#)KcMimeException.java	1.0 99/12/23
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 * 
 */
package brickst.robocust.mime;

/**
 * KcMimeException encapsulates an Exception.
 *
 */
public class KcMimeException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -984699251647507081L;
	
	private Exception e;

	KcMimeException(Exception e)
	{
		this.e = e;
	}

    /**
     * Returns a String object representing this object.
     */
    public String toString()
    {
        return e.toString();
    }
}
