/**
 * @(#)DefaultUrlHandler.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

//import rubric.server.EmailConnect.http.*;
import brickst.robocust.lib.*;
import org.apache.log4j.Logger;
import java.io.*;
import java.net.*;

/**
 * An implementation of UrlHandler. This class is used to handle a link
 * in a marketing message. The default implementation is to just fetching 
 * the url.
 *
 * @author Jim Mei
 */
public class DefaultUrlHandler
	implements UrlHandler  //, VcReportProvider
{
	static Logger logger = Logger.getLogger(DefaultUrlHandler.class);

    String baseUrl;
    double random;
    private boolean followRedirects = true;
    private VcCounter counter = new VcCounter();
    
    public DefaultUrlHandler() {};
    
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getBaseUrl() { return baseUrl; }
    public void setFollowRedirects(boolean followRedirects) { this.followRedirects = followRedirects; }
    public void setRandom(double random) { this.random = random; }
    public double getRandom() { return this.random; }
    
    public boolean handle(String stLink)
    {
        if (stLink == null)  {
            return false;
        }
            		
        // check if this is the link that we should click on, if not, do nothing
        if (CRMString.indexOfIgnoreCase(stLink, baseUrl) < 0) {
	    logger.debug("clickLink: it is not for this handler. (Base:"+baseUrl+")" +stLink);
	    return false;
        }
            		
        try {
            //determine if we really want to do the click
            double rand = Math.random();
            if (rand < this.random) {
                //calculate latency
                long beginTime = System.currentTimeMillis();
                VCLogger.getInstance().logClick(stLink);
   
                //HttpUrl url = new HttpUrl(stLink, ProxyHost.NULL_PROXY);
                URL url = new URL(stLink);
                HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();
                //url.setFollowRedirects(followRedirects);
                httpUrlConn.setInstanceFollowRedirects(followRedirects);
                
                //HttpResponse response = url.getResponse();
                httpUrlConn.connect();

                long endTime = System.currentTimeMillis();
                counter.increment(endTime - beginTime);
                
                postHandleResponse(httpUrlConn);
                return true;
            }         
        } 
        catch (MalformedURLException ex) {
        	logger.error(ex.getMessage(), ex);
        } 
        catch (IOException ex) {
        	logger.error(ex.getMessage(), ex);
        }
        return false;
    }
    
    //no-op function, override by children
    protected void postHandleResponse(HttpURLConnection connection)
    	throws IOException
    {
    }

    public void getStatsReport(StringBuffer buff) 
    {
        buff.append(counter.doReport());
    }
}
