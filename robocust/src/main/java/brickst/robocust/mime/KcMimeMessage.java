/*
 * @(#)KcMimeMessage.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.mime;

import brickst.robocust.lib.*;
import brickst.robocust.smtp.*;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * KcMimeMessage encapsulates JavaMail's MIME implementation to 
 * parse/validate/store MIME message, it hide this implementation from
 * other KC classes. <br>
 *
 * All the methods are protected.  We want only subclasses using our methods.
 * Other code should go through subclasses to ask MIME questions.  In KC,
 * we are not going to instantiate and use this Object directly; we are
 * going to use its decendant classes (ReceiverMessage, SimpleEmailMessage,
 * VCMessage), which could access KcMimeMessage's protected methods. <p>
 *
 * KcMimeMessage implements Serializable so that it could be serialized and
 * passed around among different components.
 *
 * @see com.kana.connect.common.net.smtp.SmtpMessage
 * 
 * @author Jim Mei
 */
public class KcMimeMessage extends SmtpMessage
	implements Serializable
{
	static Logger logger = Logger.getLogger(KcMimeMessage.class);
	
	/** If you move this file, please consider change this variable */
	static final long serialVersionUID = 5600795688864568548L;
	 
	/** The JavaMail message */
	protected transient MimeMessage jMessage;
	
	/** variable for outbound bodies */
	private String textBody;
	private String htmlBody;

	/** The default JavaMail session */
	private static Session jSession;
	private String charset;
    public static final String CONTENT_TYPE = "CONTENT-TYPE";

    /** charset for message header */
    private String headerCharset;
    /** charset for message body */
    private String bodyCharset;

	/**
	 * Constructor
	 */
	protected KcMimeMessage() 
	{	
		jMessage = new MimeMessage(getSession());
	}
	
	/**
	 * Constructor.
	 * Package access only.
	 *
	 * @param message	a SmtpMessage
	 */
	protected KcMimeMessage(SmtpMessage message)
	{
		//super();
		//keep SmtpMessage information
		//REVIEW: combine the following code with next constructor
		// where it takes a byte[]
		this(message.getData());
		/* this.setData(message.getData());
		ByteArrayInputStream inputStream = 
				new ByteArrayInputStream(message.getData());
		try	{
			jMessage = new MimeMessage(getSession(), inputStream);
		} catch (MessagingException e) {
			Debug.MIME.println("Exception in KcMimeMessage constructor: " + 
								e + com.brickstreet.common.lib.Util.newLine +
								Util.getStackTrace(e) + com.brickstreet.common.lib.Util.newLine +
								"SmtpMessage: " + message);
		} */
		this.setEnvelopeSender(message.getEnvelopeSender());
		this.setEnvelopeRecipients(message.getEnvelopeRecipients());
	}


    /**
	 * Constructor.
	 * Package access only.
	 *
	 * @param message	a SmtpMessage
	 */
/*
	protected KcMimeMessage(SMPPRequest request)
	{
         //TODO: What to do if message returned is null
        this (request.getMessageText().getBytes());

	    this.setEnvelopeSender(request.getSource().getAddress());

        String [] recipients = {request.getDestination().getAddress()};
        this.setEnvelopeRecipients(recipients);
	}
*/
	
    /**
	 * Constructor.
	 * Package access only.
	 *
	 * @param content	a SmtpMessage
	 */
	protected KcMimeMessage(byte[] content) 
	{
		//super();
		//keep SmtpMessage information
		//this.setEnvelopeSender(message.getEnvelopeSender());
		//this.setEnvelopeRecipients(message.getEnvelopeRecipients());
		this.setData(content);
		ByteArrayInputStream inputStream = 
				new ByteArrayInputStream(content);
		try	{
			jMessage = new MimeMessage(getSession(), inputStream);
		} catch (MessagingException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * set RFC 822 From address. We only support one from address
	 *
	 * @param address  email address
	 *
	 * @exception com.kana.connect.common.mime.KcMimeException
	 */
	protected void setFrom(String address) throws KcMimeException
	{
		setFrom(address, null);
	}
	
	/**
	 * set RFC 822 From address. We only support one from address
	 *
	 * @param address   sender email address
	 * @param name		sender name; or null.
	 *
	 * @exception com.kana.connect.common.mime.KcMimeException
	 */
	protected void setFrom(String address, String name)
		throws KcMimeException
	{
		try {
			InternetAddress iAddresses;
			if (name != null) {
				String charset = headerCharset;
                if(headerCharset == null) {
                	// Use the same charset as Mail Sender
                	charset = SystemConfig.getInstance().getProperty(SystemConfig.MessageHeaderCharset);
                }
				iAddresses = new InternetAddress(address, name, charset);
			} else {
				iAddresses = new InternetAddress(address);
			}
			jMessage.setFrom(iAddresses);
			super.setEnvelopeSender(address);
		} catch (javax.mail.MessagingException ex) {
			throw new KcMimeException(ex);
		} catch (java.io.UnsupportedEncodingException ex) {
			throw new KcMimeException(ex);
		}
	}
	
	/**
	 * add To addresses. Also adds SMTP envelope recipients.
	 *
	 * @param addresses	VArray of recipients
	 *
	 * @exception com.kana.connect.common.mime.KcMimeException
	 */
	protected void addRecipients(ArrayList<String> recipients)
		throws KcMimeException
	{
		String[] smtpRecipients = new String[recipients.size()];
		InternetAddress iAddresses[] = 
				new InternetAddress[recipients.size()];
		try {
			for (int i = recipients.size(); i-- > 0; ) {
				smtpRecipients[i] = recipients.get(i);
				iAddresses[i] = new InternetAddress(smtpRecipients[i]);
			}
			super.setEnvelopeRecipients(smtpRecipients);
			jMessage.addRecipients(
					Message.RecipientType.TO, iAddresses);
		} catch (javax.mail.MessagingException ex) {
			throw new KcMimeException(ex);
		}
	}
	
	/**
	 * Return message subject
	 * 
	 * @return String	the message subject. null if no subject or
	 *					have a MessagingException.
	 */
	protected String getSubject() 
	{
		try {
			if (jMessage == null) return null;
			String subject = jMessage.getSubject();
			//since Java Mail did not do unfolding, let's do it
			subject = unfolding(subject);
			return subject;
		} catch (MessagingException e)	{
			return null;
		} 
	}
	
	/**
	 * Do RFC822 unfolding
	 */
	private String unfolding(String msgStr)
	{
	    if (msgStr == null) return null;
        String line = null;
        StringBuffer buff = new StringBuffer();
        int startIndex = 0;      
        int endIndex = msgStr.indexOf('\n', startIndex);
        if (endIndex != -1) {
            line = msgStr.substring(startIndex, endIndex);
            if (line.length() > 0 && line.charAt(line.length() -1) == '\r') 
                line = line.substring(0, line.length() -1);
        } else {
            line = msgStr;
            endIndex = msgStr.length();
        }
        while (startIndex < msgStr.length()  && 
        		!line.equals("") ) {
            //REVIEW: we decide to only take out "\t", ignore " "
            //unfolding
            if (line.charAt(0) == '\t') 
                buff.append (line.substring(1));
            else     
                buff.append(line);
            // }

            startIndex = endIndex + 1;
            if (startIndex < msgStr.length()) {
                endIndex = msgStr.indexOf('\n', startIndex);
                if(endIndex != -1) {
                    line = msgStr.substring(startIndex, endIndex);
                    if (line.length() > 0 && line.charAt(line.length() -1) == '\r') 
                        line = line.substring(0, line.length() -1);
                } else {
                    line = msgStr.substring(startIndex);
                    endIndex = msgStr.length();
                }
            }
        }
        return buff.toString();
	}
	
	/**
	 * Set message subject. We don't care if it fails.
	 *
	 * @param subject  message's subject.
	 */
	protected void setSubject(String subject) 
	{
		try {
			String charset = headerCharset;
            if(charset == null) {
            	// Use the same charset as Mail Sender
            	charset = SystemConfig.getInstance().getProperty(SystemConfig.MessageHeaderCharset);
            }
			jMessage.setSubject(subject, charset);
		} catch (MessagingException e)	{
			//we don't care if it fails to set subject, only
			//log it for debug purpose
			logger.error(e.getMessage(), e);
		} 
	}

	/**
	 * Return the Charset of MIME content
	 *
	 * @return String	Charset, null if no Charset specified
	 */
	public String getCharset() 
	{
		if (charset == null) {
			String contentType = getHeaderFieldValue(CONTENT_TYPE);
			// Example: ContentType: text/plain; charset="Shift-JIS"
			if (contentType != null) {
				String[] attributes = contentType.split(";");
				for (int i=0; i < attributes.length; i++) {
					// Split up in name and value
					int index = attributes[i].indexOf('=');
					// Is this a charset parameter?
					if (index > 0  &&
							((attributes[i].substring(0, index)).trim()).equalsIgnoreCase("charset")) {
						charset = (attributes[i].substring(index+1)).trim();
						// Make sure we remove the quotes if it is a quoted string
						if (charset.length() > 0 && charset.charAt(0) == '"')
							charset = charset.substring(1, charset.length()-1);
						break;
					}
				}
			}
		}
		return charset;
	}
	
    /**
     * Return the value of a header field of this message(E.g. subject).
     *
     * @param fieldName 	name of the field
     * @return String		value of a header field, null if field not
     *						exists or no value. 
     *						For field with multiple values, 
     *						it returns a String with values separated by ';'
     */
    protected String getHeaderFieldValue(String fieldName)
    {
    	String values = null;
    	try {
    		if (jMessage == null) return null;
    		values = jMessage.getHeader(fieldName, ";");
    	} catch (MessagingException ex) {
    		logger.error(ex.getMessage(), ex);
    		values = null;
    	} 
    	
    	return values;
    }
   
    /**
     * Set header field of a message. Encapsulate Java Mail's method 
     * to set header field.
     *
     * @param fieldName 	name of the field
     * @param fieldValue	value of the field
     *
	 * @exception com.kana.connect.common.mime.KcMimeException
     */
    protected void setHeaderField(String fieldName, String fieldValue)
    		throws KcMimeException
    {
    	try {
    		if (jMessage == null) //return false;
    			throw new KcMimeException(
    					new Exception("jMessage is null."));
    		jMessage.addHeader(fieldName, fieldValue);
    	} catch (MessagingException me) {
    		throw new KcMimeException(me);
    	} 
    }
   
    /**
     * this method will return primary body part of
     * a message. <p>
     * 1) If the message content is text/plain, it should
     * return text/plain.
     * 2) If the message content is MPA (For example: text/plain 
     * and text/html), it should return the first text/* part of MPA.
     * 3) If the message content is text/html, it should return 
     * text/html
     *
     * @return KcMimeBodyPart  return the first text (text/*) body,
     *						   null if there's no text body.
     */
    protected KcMimeBodyPart getFirstTextBody()
    {
		try {
			MimePart bpart = null;
			if (jMessage == null) {
				//jMessage not there, use getData();
				ByteArrayInputStream inputStream = 
					new ByteArrayInputStream(getData());
				bpart = new MimeBodyPart(inputStream);
			} else {
				bpart = getPart(KcMimeBodyPart.TEXT);
			}
			return new KcMimeBodyPart(bpart, this);
		} catch (MessagingException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
    }
 
    /**
     * Return text body of the message as a String.
     *
     * @return String	Text/Plain body of the message, null if there
     *					is no HTML body.
     */
    protected String getPlainTextBody()
    {
		MimePart part = getPart(KcMimeBodyPart.TEXT_PLAIN);
		if (part != null) {
			try	{
				return ((String) part.getContent());
			} catch (MessagingException ie) 	{
				logger.error(ie.getMessage(), ie);
			} catch (IOException e) 	{
				logger.error(e.getMessage(), e);
			}
		}
		return null;
    }
    
    /**
     * Return html body of the message as a String.
     *
     * @return String	HTML body of the message, null if there
     *					is no HTML body.
     */
    protected String getHtmlBody()
    {
		MimePart part = getPart(KcMimeBodyPart.TEXT_HTML);
		if (part != null) {
			try	{
				return ((String) part.getContent());
			} catch (MessagingException ie) 	{
				logger.error(ie.getMessage(), ie);
			} catch (IOException e) 	{
				logger.error(e.getMessage(), e);
			}
		}
		return null;
    }

    /**
     * Return Report body of the message as an InputStream
     *
     * @return String	Report body of the message, null if there
     *					is no Report body.
     */
    public InputStream getReportBody ()  {
    	try {
			MimePart part = getPart(KcMimeBodyPart.MESSAGE_DELIVERY_STATUS);
			if (part != null)
				return part.getInputStream();
			else
				return null;
		}
		catch (MessagingException ie) 	{
			logger.error(ie.getMessage(), ie);
		}
		catch (IOException ex) 	{
			logger.error(ex.getMessage(), ex);
		}
		return null;
    }
    
    /**
     * Private method to return either HTML content or TEXT content.
     *
     * @param	contentType		should be one of text/plain, text/html 
     *							or text/*
     *
     * @return	MimeBodyPart	required content as MimeBodyPart, return null
     *							if not found.
     */
    private MimePart getPart(String contentType) 
    {
		try {
		    if (jMessage == null) return null;
			MimeBodyPart bpart = null;
			
			if (jMessage.isMimeType(contentType)) {
			    return jMessage;
				/* Object obj = jMessage.getContent();
				if (obj instanceof String ) {
					bpart = new MimeBodyPart();
					bpart.setContent(obj, jMessage.getContentType());
				} else if (obj instanceof ByteArrayInputStream) {
					bpart = new MimeBodyPart((java.io.ByteArrayInputStream) obj);
				} else {
					//this method is private, it is only used to get
					//text or html message, so we should never be here
					Debug.kcassert(false);	
				}
				return bpart; */
			} else if (jMessage.isMimeType(KcMimeBodyPart.MULTIPART)) {
				//content must be a multipart
				MimeMultipart mparts = (MimeMultipart) jMessage.getContent();;
				return (getBody(mparts, contentType));
			}
		} catch (MessagingException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
    }
  
    /**
     * Private method to return a primary type content out of multipart message.
     * A primary type is either text/plain, text/html or text/*. It is specified
     * by param contentType.
     *
     * @param   mPart			a JavaMail MimeMultipart object
     * @param	contentType		should be one of text/plain, text/html 
     *							or text/*
     *
     * @return	MimeBodyPart	a MimeBodyPart for required content type, return
     *							null if not found.
     */
    private MimeBodyPart getBody(MimeMultipart mparts, String contentType) 
    {
		try {
			MimeBodyPart bpart = null;
			for (int i = 0; i < mparts.getCount() ; i++) {
				bpart = (MimeBodyPart) mparts.getBodyPart(i);
				if (bpart.isMimeType(contentType) )
					return bpart;
				else if (bpart.isMimeType(KcMimeBodyPart.MULTIPART)) {
					MimeMultipart mpart = (MimeMultipart)
												bpart.getContent();
					//recersively parse until we find text
					bpart = getBody(mpart, contentType);
					if (bpart != null)
						return bpart;
				}
			}
		} catch (MessagingException ex) {
			logger.error(ex.getMessage(), ex);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return null;
    }
	
	/**
	 * Set the primary text body.  <br>
	 *
	 * We will encapsulate how to construct MIME message
	 * from outside world here, like what is content type, etc.
	 * 
	 * @param body in String format
	 */
	protected void setPlainTextBody(String body) 
	{ 
		this.textBody = body;
	}
 
	/**
	 * Set the html body. <br>
	 * 
	 * We will encapsulate how to construct MIME message
	 * from outside world here, like what is content type, etc.
	 *
	 * @param body in String format
	 */
	protected void setHtmlBody(String body) 
	{ 
		this.htmlBody = body;
	}
 
	/**
	 * private method to get session (default session)
	 *
	 * @return Session
	 */
	private Session getSession()
	{
		if (jSession == null) {
		    jSession = Session.getDefaultInstance(System.getProperties(), null);
		}
		return jSession;
	}
	
	/**
	 * Check if this message contains a valid MimeMessage. <br>
	 * Remember the constructor of this object will take either
	 * an input stream, or SmtpMessage. Either way it is not parsed
	 * to Mime structure. This method will check if the message
	 * it contains is a Mime structured message or not.
	 *
	 * @return true if it is a MimeMessage, otherwise false.
	 */
	protected boolean isValidMimeMessage()
	{
		return(jMessage != null);
	}   
	
	/** 
	 * Read object from an object input stream (fro Serialization).
	 * MimeMessage is not serializable, so we need to reconstruct
	 * it after serialization.
	 */
    private void readObject(ObjectInputStream input)
        throws IOException, ClassNotFoundException
	{
        // defaultReadObject must be called before our own data serialization
        input.defaultReadObject();

		byte[] bytes = getData();
		if (bytes == null) {
			jMessage = new MimeMessage(getSession());
			return;
		}
		
		ByteArrayInputStream inputStream = 
				new ByteArrayInputStream(bytes);
		try	{
			jMessage = new MimeMessage(getSession(), inputStream);
		} catch (MessagingException ex) {
			logger.error("In readObject: " + ex.toString(), ex);
		}
	} 

	/**
	 * Writes the complete message to the OutputStream out.
     *
     * @param out		the OutputStream to write the message to
     * @param redirect	a boolean to indicate whether this is a redirect
     *					or normal send
     * 
     * @exception java.io.IOException
	 */
	protected void writeTo(OutputStream out, boolean redirect) 
		throws java.io.IOException
	{
		if (redirect) {
			//for redirect, use SmtpMessage's writeTo()
			if (logger.isDebugEnabled()) {
				logger.debug("Redirect message. " + super.getData());
			}
			super.writeTo(out);
		} else {
			try {
				//before we output jMessage, we need to compose its content
				composeMessage();

				jMessage.writeTo(out);
				if (logger.isDebugEnabled()) {
					logger.debug("Sending jmessage. " + this);
				}
			} catch (javax.mail.MessagingException ex) {
				logger.error(ex.toString(), ex);
			} catch (Exception ex) {
				logger.error(ex.toString(), ex);
				throw new IOException(ex.toString());
			}
		}
	}
	
	/**
	 * compose message before sending
	 */
	private void composeMessage()
	{
        String charset = bodyCharset;
        if(bodyCharset == null) {
		// Use the same charset as Mail Sender
        	charset = SystemConfig.getInstance().getProperty(SystemConfig.MessageHeaderCharset);
        	if (charset == null) {
        		charset = "iso-8859-1";
        	}
        }
		String charsetSpecifier = "; charset=" + 
				MimeUtility.quote(charset, HeaderTokenizer.MIME);
		if (jMessage == null) 
			jMessage = new MimeMessage(getSession());
					
		try	{
			if(textBody != null && htmlBody != null) {
				//if both body exists, compose MPA
				MimeBodyPart text = new MimeBodyPart();
				text.setContent(textBody, KcMimeBodyPart.TEXT_PLAIN + charsetSpecifier);
				MimeBodyPart html = new MimeBodyPart();
				html.setContent(htmlBody, KcMimeBodyPart.TEXT_HTML + charsetSpecifier);
				MimeMultipart mpart = new MimeMultipart();
				mpart.setSubType(KcMimeBodyPart.MPA_ALTERNATIVE_SUBTYPE);
			
				mpart.addBodyPart(text);
				mpart.addBodyPart(html);
				jMessage.setContent(mpart);
			} else if (textBody != null) {
				//only textBody exists, compose plain text
				jMessage.setText(textBody, charset);
			} else if (htmlBody != null) {
				//only textBody exists, compose text/html text
				jMessage.setContent(htmlBody, KcMimeBodyPart.TEXT_HTML + charsetSpecifier);
			}
			
			//set date if it has not been set
			if (jMessage.getSentDate() == null) 
				jMessage.setSentDate(CRMTime.getCurrentDate());
			
			jMessage.saveChanges();
		} catch (MessagingException ex) {
			logger.error(ex.toString(), ex);
		} catch (Exception ex) {
			logger.error(ex.toString(), ex);
		}			
	}


	/**
	 * return a String representation of this object
	 *
	 * @return string representation of this address
     * 
	 */
	public String toString()
	{
		try {
			if (jMessage == null)
				return new String(getData());
			else {
				ByteArrayOutputStream ba = new ByteArrayOutputStream();
				jMessage.writeTo(ba);
				return ba.toString();
			}
		} catch (IOException e) {
			return e.toString();
		} catch (MessagingException me) {
			return me.toString();
		}
	}

    /**
     * setter for headerCharset
     *
     * @param charset String
     */
    public void setHeaderCharset(String charset) {
        headerCharset = charset;
    }

    /**
     * setter for bodyCharset
     *
     * @param charset String
     */
    public void setBodyCharset(String charset) {
        bodyCharset = charset;
    }
}
