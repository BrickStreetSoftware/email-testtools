/*
 * @(#)SmtpInputStream.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 * 
 */
package brickst.robocust.smtp;

import java.net.*;
import java.io.*;

/**
 * SmtpInputStream class is a package class and is used to read data 
 * from SMTP session. <p>
 *
 * This class contains an InputStream to read data from. For performance 
 * reason, when we read data from input SMTP session, we want to save as byte
 * array. Currently the readline() method of this class will return String since
 * it is most used by <p>
 *
 * @author  Jim Mei
 * @version 1.0 1/25/00
 */
final class SmtpInputStream
{
    /** constant */
    public final String SMTP_NEW_LINE = "\r\n";
    /** max size of a message */
    private static long MAX_MESSAGE_SIZE = 0;

    private InputStream 	in;
    private AsciiString buffer = new AsciiString(512);  //reusable buffer

    /**
    * Constructor. 
    *
    * @param in  an InputStream
    */
    SmtpInputStream(InputStream in)
    {
        if (MAX_MESSAGE_SIZE == 0) {
            //REVIEW: fetch from config file
            MAX_MESSAGE_SIZE = 10000000;
        }
	this.in = in;
    }
    	
    /**
    * this method will close InputStream
    */
    void close() 
    {
        buffer = null;
		if (in != null) {
			try {
				in.close();
			}
			catch (IOException iox) {}
			in = null;
		}
    }
    	
    private int prevReadChar = -1;
    /**
    * Read a line containing only ASCII characters from the input 
    * stream. A line is terminated by a CR or NL or CR-NL sequence. 
    * The line terminator is not returned as part of the returned 
    * String. Returns null if no data is available. <p>
    *
    * @param in  a InputStream
    *
    * @return String	a line read from inputStream, return null if
    *					hit end of file before read any chars. <br>
    * Should we return null for SocketException and 
    *					InterruptedIOException? 
    * @exception IOException
    */
    String readLine() throws IOException 
    {
        //clean the buffer
        int c1 = prepareBuffer(); 
        
        if (c1 == '\r') {
            // Got CR, is the next char NL ?
            int c2 = in.read();
            if (c2 != '\n') {
                prevReadChar = c2;
            }
        } else {
            while ((c1 = in.read()) != -1) {
                if (c1 == '\n')  {	// Got NL, outa here.
                    break;
                } else if (c1 == '\r') {
                    // Got CR, is the next char NL ?
                    int c2 = in.read();
                    if (c2 != '\n') {
                        prevReadChar = c2;
                    }
                    break; // outa here.
                }
                			
                // Not CR, NL or CR-NL ...
                // .. Insert the byte into our byte buffer
                buffer.append((byte) c1);
            }
        }

        if (c1 == -1 && buffer.length() == 0) 
            return null;
            		
        return buffer.toString();
    }   
    
    private int prepareBuffer()
    {
        if (buffer == null) 
            buffer = new AsciiString(512);
        else 
            buffer.clear();
            
        //check if there's another left from last read
        int prevChar = prevReadChar;
        if (prevReadChar != -1) {
            buffer.append((byte) prevReadChar);
            prevReadChar = -1;
        }
        return prevChar;
    }
    	
    /**
    * Read a SMTP data from the input steam containing only 8 bit 
    * characters. The SMTP data is terminated by a NL+'.'+NL or 
    * NL+'.'+CR+NL. Returns null if no data is available. <p>
    *
    * @param in  a InputStream
    *
    * @return String	a line read from inputStream, return new byte[0] if
    *					hit end of file before read any chars
    * @exception IOException
    */
    byte[] readData() throws IOException 
    {
        //clean up buffer
        prepareBuffer();
        int c1;

        while ((c1 = in.read()) != -1) {
	        //REVIEW: handle begin with ".\n"
            if (c1 =='.' && buffer.length() == 0) {
	        //handle begin with ".\n"
	        // let's find end '\n' or '\r\n'
	        int c3 = in.read();
	        if (c3 == '\n') { // find  begin with ".\n", break
                    break;
	        } else if (c3 == '\r') { //find "\n.\r", check if next is '\n'
                    int c4 = in.read();
                    if (c4 == '\n') //find "\n.\r\n", break
                        break;
                    else {
                        append((byte) c1); // .. Insert the byte into our byte buffer
                        append((byte) c3); // .. Insert the byte into our byte buffer
                        append((byte) c4); // .. Insert the byte into our byte buffer
                    }
                } else { 
                    // it start with ".*", * is a char other than "\r" and "\n"
                    // based on RFC we should remove the "."
                    append((byte) c1); // .. Insert the byte into our byte buffer
                    //buffer.append(c2); // .. Insert the byte into our byte buffer
                    append((byte) c3); // .. Insert the byte into our byte buffer
                }    		
            } else if (c1 == '\n') { // Got NL, is the next char is '.' 
	        int c2 = in.read();
	        if (c2 == '.') {
                    // find '\n'+'.', now let's find end '\n' or '\r\n'
                    int c3 = in.read();
                    if (c3 == '\n') { // find "\n.\n", break
                        break;
                    } else if (c3 == '\r') { //find "\n.\r", check if next is '\n'
                        int c4 = in.read();
                        if (c4 == '\n') //find "\n.\r\n", break
	                        break;
                        else {
	                        append((byte) c1); // .. Insert the byte into our byte buffer
	                        append((byte) c2); // .. Insert the byte into our byte buffer
	                        append((byte) c3); // .. Insert the byte into our byte buffer
	                        append((byte) c4); // .. Insert the byte into our byte buffer
                        }
                    } else { 
                        // it start with "\n.*", * is a char other than "\r" and "\n"
                        // based on RFC we should remove the "."
                        append((byte) c1); // .. Insert the byte into our byte buffer
                        //buffer.append(c2); // .. Insert the byte into our byte buffer
                        append((byte) c3); // .. Insert the byte into our byte buffer
                    }
                } else {
                    append((byte) c1); // .. Insert the byte into our byte buffer
                    append((byte) c2); // .. Insert the byte into our byte buffer
	        }
        } else {   	
	        append((byte) c1); // .. Insert the byte into our byte buffer
        }
        }

        int length = buffer.length();
        //if ((c1 == -1) && (length == 0))
        if (length == 0)
        return new byte[0];
        if (buffer.charAt(length -1) == '\r') { //means we found "\n.\r\n", "\n.\n" it  
                                                //does not make sense to end a file 
                                                //with '\r'
        buffer.setLength(length - 1);
        }

        //get byte[] from buffer
        return buffer.getBytes();
    }
        
    /**
    * private method, append char to buffer.
    * throws IPException if reached max message size.
    *
    * @param b  byte to append
    */
    private void append(byte b) throws IOException
    {
        if (buffer.length() >= MAX_MESSAGE_SIZE) 
            throw new IOException("Exceeds max message size");
        buffer.append(b);
    }
}
