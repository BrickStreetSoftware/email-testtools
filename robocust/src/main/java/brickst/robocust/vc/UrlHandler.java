/**
 * @(#)UrlHandler.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

/**
 * Interface defines the method(s) needs to be implemented by UrlHandler.
 * UrlHandler will be used by VC to handle (click) links in a marketing message.
 *
 * @author Jim Mei
 */
interface UrlHandler extends IRandom, VcReportProvider
{
    /** 
     * handle url. 
     * @param url
     * @return boolean   true if url is handled, false otherwise
     */
    public boolean handle(String url);
    
    public void setBaseUrl(String baseUrl);
    public String getBaseUrl();
    public void setFollowRedirects(boolean followRedirects);
}
