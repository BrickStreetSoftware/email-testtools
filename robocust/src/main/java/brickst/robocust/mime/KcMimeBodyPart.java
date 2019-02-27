/*
 * @(#)KcMimeBodyPart.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.mime;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimePart;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.apache.log4j.Logger;

/**
 * KCBodyPart is a wrapper class of Java Mail's MimeBodyPart, it
 * encapsulates Java Mail's parsing/storing mechnism and 
 * only exposes content (primary body) and content_type 
 * information to user. <p>
 * 
 * @author Jim Mei
 */
public class KcMimeBodyPart 
{
	static Logger logger = Logger.getLogger(KcMimeBodyPart.class);
	
	private MimePart part = null; 
    private KcMimeMessage parent = null; 
    
    /** Constant for content-type */
    public static final String TEXT = "text/*";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String MESSAGE_DELIVERY_STATUS = "message/delivery-status";    
    public static final String TEXT_HTML = "text/html";
    public static final String MULTIPART = "multipart/*";
    public static final String MULTIPART_ALTERNATIVE = 
    									"multipart/alternative";
    public static final String MPA_ALTERNATIVE_SUBTYPE = 
    									"alternative";
    
	/**
	 * Constructor.
	 *
	 * @param part  a MimeBodyPart
	 */
	KcMimeBodyPart(MimePart part, KcMimeMessage parent) 
	{
		super();
		this.part = part;
		this.parent = parent;
	}

	/**
	 * Returns the message content as String.
	 *
	 * @return message content as String, null if failed to get content.
	 */
	public String getContent() 
	{
		return getContent(this.part);
	}
   
	private String getContent(Part part) 
	{
		try	{
			if (this.part == null) 
				return null;
			
			Object content = part.getContent();
			
			if (content instanceof String)
				return (String) content;
			else if (content instanceof InputStream) {
				logger.debug("content as InputStream " + content);
				return getContentFromInputStream((InputStream) content);
			} else if (content instanceof Multipart) {
				//I believe this is impossible
				logger.debug("multipart as content " + content);
				return getContentFromMultipart((Multipart) content);
			} else {
				//Debug.assertSkip(false, "unknow MimePart " + content);
				return null;
			}
		} catch (MessagingException ie) 	{
			return null;
		} catch (IOException e) 	{
			return null;
		}
	}
   
    private String getContentFromMultipart(Multipart mp)
    {
    	BodyPart firstBodyPart = null;
    	try {
    		firstBodyPart = mp.getBodyPart(0);
    	} catch (MessagingException me) {
    		logger.error(me.toString(), me);
    	}
    	if (firstBodyPart == null) 
    		return null;
    	
    	return getContent(firstBodyPart);
    }
    
    private String getContentFromInputStream(InputStream is)
    	throws IOException
    {
    	BufferedInputStream bis = new BufferedInputStream(is);
    	ByteArrayOutputStream bb = new ByteArrayOutputStream();
    	
    	int ch;

    	while ( (ch = bis.read()) != -1) {
    		bb.write((byte) ch);
    	}
    	
    	String charSet = parent.getCharset();
    	String content;
    	if (charSet == null) {
    		content = new String(bb.toByteArray());
    	} else {
    		try {
    			content = new String(bb.toByteArray(), charSet);
    		} catch (UnsupportedEncodingException ue) {
    			logger.error(ue.getMessage(), ue);
    			content = new String(bb.toByteArray());
    		}    		
    	}
    	return content;    	
    }
    
    /**
     *
     * Encapsulate content type detail, just provides interface
     * to check if the content type is what passed into. The format
     * of content type should be "type/subtype", like "text/plain".
     *
     * @param mimeType
     * @return true if content type match,
     *		   otherwise return false. 
     */
    public boolean isMimeType(String mimeType)
    {
    	try {
    		if (part == null) return false;
    		return (part.isMimeType(mimeType));
    	} catch (MessagingException ex) {
    		logger.error(ex);
    		return false;
    	}
    }
}
