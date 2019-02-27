/*
 * @(#)VCMessage.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;
import brickst.robocust.mime.*;
import brickst.robocust.smtp.*;
import java.util.ArrayList;
import org.apache.log4j.Logger;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

/**
 * VCMessage is a package class and is used only in VC package. It
 * extends KcMimeMessage to access kcMimeMessage's protected method
 * and manipulation of mime message parsing/storing.
 *
 * @see com.kana.connect.common.mime.KcMimeMessage
 *
 * @author  Jim Mei
 * @version 2.0 1/19/00
 */
final class VCMessage extends KcMimeMessage
{
	static Logger logger = Logger.getLogger(VCMessage.class);
	private static final long serialVersionUID = -6239592648738858166L;

    //private MessageContext context = null; // derived from From: address; see getContext()
    
    /**
     * constructor
     */
    public VCMessage(SmtpMessage msg) 
    {
        super(msg);
	if (!isValidMimeMessage())
	    logger.error("Mail Composer generated invalid MIME message: " + msg);

        //String from = getEnvelopeSender();
        //if (from != null) {
    	//	context = new MessageContext();
    	//	context.parseContextFromString(from);
    	//}
    }
    
    /** 
     * Return the size of message. This is used for debug purpose.
     * After we received an email message, VC will log it into db. <br>
     *
     * The size of a message is the size of content (byte array in SmtpMessage).
     * 
     * @return long  size of the message. 
     */
    public long getSize() { return(getData().length); }
    
	/**
	 * This method will stripe all URLs from a text content 
	 *
	 * @param text	input String needs to be parsed
	 *
	 * @return VArray	hold all values found 
	 */
	public static final ArrayList<String> parseTextUrls(String text)
	{
		ArrayList<String> vector = new ArrayList<String>();
		CRMString lastOccurance = parseTextUrl(text, 0);
		while (lastOccurance != null) {
			// only add to vector if length > 0, in case we have "<>"
			if (lastOccurance.length() > 0) {
				vector.add(lastOccurance.toString());
			}

			int startIndex = lastOccurance.getEndPosition();
			lastOccurance = parseTextUrl(text, startIndex);
		}

		return vector;
	}

	/**
	 * This method will stripe URL from a text content 
	 *
	 * @param text	input String
	 * @param int	begin point
	 *
	 * @return a URLLink, possibly for an empty link (eg, "http://").
	 */
	public static final CRMString parseTextUrl(String text, int begin)
	{
		//to us, url should be something begin with
		// http:// or ftp://, and end with either " " or "new line"

		// FUTURE: private static final constants: http, ftp, https.  Walk array
		String http = "http://";
		String https = "https://";
		String ftp = "ftp://";
		char httpLeftCh = http.charAt(0); 
		char ftpLeftCh = ftp.charAt(0);
		char httpsLeftCh = https.charAt(0);
		// UNUSED! char endCh = ' ';
		// it's impossible we have "ftp://EOF".  
		// REVIEW1: Explain.
		// REVIEWED: we search link from "begin" to text.length - https.length(),
		//          this means, if there's something "ftp://EOF" we will not be
		//          able to catch. However, we don't care about ftp:// since
		//          it is not a valid link any way. 
		int textLength = 0;
		int	iMax = textLength - https.length(); 
		int httpSize = http.length();
		int ftpSize = ftp.length();
		int httpsSize = https.length();
		
		if (text != null) {
			textLength = text.length();
		}
		
		char nextCh = ' ';
		for (int i = begin; i < iMax; i++) {
			nextCh = Character.toLowerCase(text.charAt(i));
			// FUTURE: Abstract these if clauses!
			// FUTURE: Use case insensitive search function in CRMString.
			if (nextCh == httpLeftCh) {
				if (text.substring(i, 
						i+httpSize).toLowerCase().equals(http)) {
					// found http mark, let's find the end,
					// the end is either newline or ' '
					//if no new line, use EOF

					// Determine first whitespace after http string.
					int j = i+httpSize;
					for (; j < textLength; j++) {
						if (!isUrlCharacter(text.charAt(j)))
							break;
					}
					return(new CRMString(text.substring(i, j), i, j));
				} else if (text.substring(i, 
						    i+httpsSize).toLowerCase().equals(https)) {
				    // REVIEW1: We'll never get here.  Just remove elses?
				    // REVIEWED: I see. Moved here from "else if".
					// found https mark
					int j = i+httpsSize;
					for (; j < textLength; j++) {
						if (!isUrlCharacter(text.charAt(j)))
							break;
					}						  
					return(new CRMString(text.substring(i, j), i, j));
				}
			} else if (nextCh == ftpLeftCh) {
				if (text.substring(i, 
						i+ftpSize).toLowerCase().equals(ftp)) {
					// found ftp mark
					int j = i+httpSize;
					for (; j < textLength; j++) {
						if (!isUrlCharacter(text.charAt(j)))
							break;
					}						  
					return(new CRMString(text.substring(i, j), i, j));
				}
			} 
		}
		return null;
	}

	public static boolean isUrlCharacter(char ch)
	{
		// FUTURE: Check RFC for exact list of valid characters.
		// Checked RFC, it seems only chars should not appear in URI
		// is whitespace + " + < + >
		return(!(Character.isWhitespace(ch) ||
				ch == '"'|| ch == '<' || ch == '>'));
	}
    
    /**
     * Parse links out of message
     *
     * @return String array of links. Return an empty string array if 
     * no links found.
     */
    public String[] getLinks() {
        // first check content type of this message
        KcMimeBodyPart bodyPart = super.getFirstTextBody();
        String content = bodyPart.getContent();
        //VC receive message from MC, we have to have content
        if (content == null) {
            logger.error("Message received by VC has content. " + this);
        }        
        
        ArrayList<String> links = new ArrayList<String>();
        
        if (bodyPart.isMimeType("text/html") ) {
            //
            // parse html content 
            //
            Document htmlDoc = Jsoup.parse(content);
            
            // extract hyperlinks
            Elements linkNodes = htmlDoc.select("a[href]");
            for (Element linkNode : linkNodes) {
            	String href = linkNode.attr("href"); 
            	links.add(href);
            }

            // extract image links
            Elements imgNodes = htmlDoc.select("img[src]");
            for (Element imgNode : imgNodes) {
            	String imgSrc = imgNode.attr("src"); 
            	links.add(imgSrc);
            }
            
        } 
        else {
            links = parseTextUrls(content);
        }

        // convert result to string[] and return
        String[] outLinks = (String[]) links.toArray(new String[links.size()]);
        return(outLinks);
    }
	
} // class VCMessage
