/**
 * @(#)UrlSurveyHandler.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * An implementation of UrlHandler. This class is used to handle a link
 * in a marketing message. The default implementation is to just fetching 
 * the url.
 *
 * @author Jim Mei
 */
public class UrlSurveyHandler extends DefaultUrlHandler
{
	static Logger logger = Logger.getLogger(UrlSurveyHandler.class);

    private Hashtable surveyCounters = new Hashtable();
    
    public UrlSurveyHandler() {};
    
    private static final String SURVEY_KEYWORD = "/www/submitsurvey.jsp"; 
    //override parent's no-op funtion
    protected void postHandleResponse(HttpURLConnection response)
    	throws IOException
    {
    	// read bytes from url connection to byte array
    	ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
    	InputStream urlStream = response.getInputStream();
    	while (true) {
    		int cc = urlStream.read();
    		if (cc < 0) {
    			break;
    		}
    		bytestream.write(cc);
    	}
        byte[] bytes = bytestream.toByteArray();
        
        if (bytes == null) return;
        //use default charset to convert bytes to a string
        String content = new String(bytes);
        if (logger.isDebugEnabled()) {
        	logger.debug("Post handle response: content " + content);
        }
        
        //find all forms and check if they are survey, handle them
        int beginIndex = 0;
        String contentLowerCase = content.toLowerCase();
        HtmlForm input = null;
        while ((input = HtmlForm.parseForm(content, contentLowerCase, beginIndex)) != null)
        {
            logger.debug("form: " + input.toString());
            beginIndex = input.getEndLocation();
            if (CRMString.indexOfIgnoreCase(input.getAction(), SURVEY_KEYWORD) >= 0) {
                handleSurveyForm(input, getSurveyUrl(response.getURL(), input.getAction()));
            }
        }
    }

    /**
     * this method will return a base url 
     *
     * @param url   original url
     *
     * @return base url
     */
    public String getBaseUrl(String url) 
    {
        //this is a generic public method 
        if (url == null || url.trim().length() == 0) 
            return "";
            
        
		//first find base
		String	base = url;
   		
        int index = base.indexOf('?');
		if (index != -1)
			base = base.substring(0, index);

		// "http://www.netscape.com/home/fok.html?name=value" is handled
		// by above code, indexOf('?'),
		// "http://www.netscape.com" is handled by following code, 
		// the result is  "http://www.netscape.com/". 
		index = base.lastIndexOf('/');
		if (index == -1) {
		    return base;
		}
		int sIndex = base.indexOf("://");
		if (index == sIndex + 2) //http://www.netscape.com
		    return base + "/";
		    
		return(base.substring(0, index+1));
    }

	private String addBasetoLink(String base, String link)
	{
	    // if base or location is null, return location
	    // FUTURE: do not allow null arguments
	    if (base == null || link == null)
			return link;

		//first find base
		String	sBase = getBaseUrl(base);

		// Truncate sBase after last '/'
		// REVIEW2: PreparedContent.insertBase() first chops off query string;
		// make consistent by centralizing use in EmailUrl?
		// REVIEWED: add a new method getBaseURL(String url) to centralize the
		// code

		// If location is not a relative link, return location as is.
		// REVIEW3: Is this true?  Give it a method, explain what's going on.
		// Give us an example of how this works.
		// REVIEWED: <a href="http://www.yahoo.com/index">Home</a>
		//           <a href="a?li,http://www.yahoo.com">li</a>
		// the first example is a absolute path, the second is a relative link, 
		// so search for :// but also need to find if this happens after a "?"
		// or other relative link's special chars
		
		//check if location starts with "schema://xxx"
		int index = link.indexOf("://");
		if (index >= 0 ) {
		    int qIndex = link.indexOf("?");
		    int cIndex = link.indexOf(";");
		    int sIndex = link.indexOf("/");
		    if ( (qIndex > 0 && qIndex < index)
					|| (cIndex > 0 && cIndex < index) 
					|| (sIndex > 0 && sIndex <index))
				;
		    else
		        return link;
		}

		if (link.trim().startsWith("#"))  //local link, don't add base
			return link;
		else if (link.trim().toLowerCase().startsWith("mailto:"))
			return link;

		// REVIEW: Was: Debug.kcassert(sBase.charAt(sBase.length() - 1) == '/');	// required below.
		assert (sBase.length() == 0  ||  sBase.charAt(sBase.length() - 1) == '/');	// required below.

		//if location start with "/"
		if (link.startsWith("/"))
		    return(sBase + link.substring(1));

		//if location start with "./" (current directory)
		if (link.startsWith("./"))
		    return(sBase + link.substring(2));

		// FUTURE: Fix comment: if index < 0, location is a relative path,

		// Example:
		//		content = http://www.host.com/f1/f2/f3/index.html
		//		base = http://www.host.com/f1/f2/f3/
		//		link = ../../f8/banana.html
		//		desired result = http://www.host.com/f1/f8/banana.html
		String sLocation = link;
		while (sLocation.startsWith("../")) {
			sLocation = sLocation.substring(3);
			//note, the last char of sBase is '/'
			index = sBase.lastIndexOf('/', sBase.length() - 2); // skip last '/'
			if (index > 0) {
				sBase = sBase.substring(0, index);
			}
		}

		// REVIEW: This clause is strange.  Please explain.  
		// Please ensure this example is handled properly:
		//		content = http://www.host.com/f1/f2/f3/index.html
		//		base = http://www.host.com/f1/f2/f3/
		//		link = f8/banana.html
		//		desired result = http://www.host.com/f1/f2/f3/f8/banana.html
		// REVIEWED: this clause is not necessary 
/*		index = sBase.lastIndexOf('/', sBase.length()-1);
		if (index > 0) {
			sBase = sBase.substring(0, index + 1);
		} */
		return(sBase + sLocation);
	}

    private SurveyUrl getSurveyUrl(URL url, String method)
    {
        String surl = url.toString();
        String query = null;
        int index = surl.indexOf('?');
        if (index >= 0) 
            query = surl.substring(index + 1);
        surl = addBasetoLink(surl, method);
        return new SurveyUrl(surl, query);
    }
    
    private static final String HIDE = "hidden";
    //consider to create a new service
    private void handleSurveyForm(HtmlForm form, SurveyUrl surveyUrl) 
    {
        logger.debug("Handle survey Url " + surveyUrl + ", form: " + form);
        ArrayList<HtmlFormInput> formInputs = form.getFormInputs();
        StringBuffer sb = new StringBuffer();
        String surveyID = null;
        
        for (int i = 0; i < formInputs.size(); i++) {
            HtmlFormInput formInput = formInputs.get(i);
            logger.debug(formInput.toString());  //REview:
            String type = formInput.getAttributeValue(HtmlFormInput.TYPE);
            if (type != null && type.equalsIgnoreCase(HIDE)) {
                if ( i > 0) sb.append('&');
                String sName = formInput.getAttributeValue(HtmlFormInput.NAME);
                String sValue = formInput.getAttributeValue(HtmlFormInput.VALUE);
                if (sName != null) {
                    sb.append(sName);
                    if (sName.equalsIgnoreCase("SURVEYID"))
                        surveyID = sValue;
                }
                 
                if (sValue != null)
                    sb.append('=').append(sValue);
            } 
        }
        
        if (surveyUrl.query != null)
            sb.append('&').append(surveyUrl.query);
        
        try {
            //calculate latency
            long beginTime = System.currentTimeMillis();
            //HttpUrl url = new HttpUrl(surveyUrl.url, ProxyHost.NULL_PROXY, sb.toString().getBytes());
            URL url = new URL(surveyUrl.url);
            HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();
            httpUrlConn.setRequestMethod("POST");
            httpUrlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpUrlConn.setDoOutput(true);
            
            // send post data
            DataOutputStream wr = new DataOutputStream(httpUrlConn.getOutputStream());
            wr.writeBytes(sb.toString());
            wr.flush();
            wr.close();

            InputStream is = httpUrlConn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuffer response = new StringBuffer(); 
            while (true) {
            	String line = rd.readLine();
            	if (line == null) {
            		break;
            	}          
            	response.append(line);
            	response.append("\n");
            }
            rd.close();
            
            logger.info(response.toString());
            long endTime = System.currentTimeMillis();
            getSurveyCounter("Survey " + surveyID).increment(endTime - beginTime);
        } 
        catch (MalformedURLException ex) {
        	logger.error(ex.getMessage(), ex);
        } 
        catch (IOException ex) {
        	logger.error(ex.getMessage(), ex);
        }
    }
    
    private synchronized VcCounter getSurveyCounter(String name) 
    {
        VcCounter counter = (VcCounter) surveyCounters.get(name);
        if (counter == null) {
            counter = new VcCounter(name);
            surveyCounters.put(name, counter);
        }
        return counter;
    }

    public void getStatsReport(StringBuffer buff) 
    {
        super.getStatsReport(buff);
        for (Enumeration e = surveyCounters.elements() ; e.hasMoreElements() ;) {
            VcCounter counter = (VcCounter) e.nextElement();
            buff.append("\r\n\t").append(counter.getName()).append(": ").append(counter.doReport());
        }
    }
    
    class SurveyUrl 
    {
        private String url;
        private String query;
        
        SurveyUrl(String url, String query) 
        { 
            this.url = url; this.query = query; 
        }
    }
}
