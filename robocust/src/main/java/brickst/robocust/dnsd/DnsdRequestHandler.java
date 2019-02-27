/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.dnsd;

/**
 * This interface has to be implemented to handle a DNS Request
 * and return a DNS Response.
 * @author Jing Zhou
 */
public interface DnsdRequestHandler
{
	/**
	* The implementer will generate a response for the request.
	* @param request incoming DNS request from the DNS client
	* @return the desired response to the DNS client
	*/
	public DnsdResponse handle(DnsdRequest request);

}