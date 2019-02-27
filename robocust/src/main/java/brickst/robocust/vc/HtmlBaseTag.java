/*
 * @(#)HtmlBaseTag.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import java.util.*;
import org.apache.log4j.Logger;
import brickst.robocust.lib.*;

/**
 * HtmlBaseTag is an abstract class. It has come common method shared by all HTML tag
 *
 * @author Jim Mei
 */
abstract public class HtmlBaseTag
{  
	static Logger logger = Logger.getLogger(HtmlBaseTag.class);

    private String tagName;   
    private int endLocation; //ending position in the content
    private ArrayList<Attribute> attributes;
        
    /**
    * constructor
    *
    * @param   tag
    */
    protected HtmlBaseTag(CRMString formInput)
    {
        this.setEndLocation(formInput.getEndPosition());
        parseValue(formInput.getText());
    }

    protected void parseValue(String formInput)
    {
        if (formInput.startsWith("<"))
            formInput = formInput.substring(1);
        int index = formInput.indexOf('>');
        if (index >= 0) 
            formInput = formInput.substring(0, index);
            
        attributes = new ArrayList<Attribute>();
        StringTokenizer tokenizer = new StringTokenizer(formInput, " ");
        
        //first token is tag's name
        tagName = tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            handleToken(tokenizer.nextToken());
        }
    }
    
    private void handleToken(String token) 
    {
        logger.debug("parse token " + token);
        int index = token.indexOf('=');
        if (index > 0) //we don't care if it is equal to 0
        {
            String lName = token.substring(0, index).trim().toUpperCase();
            String lValue = token.substring(index + 1);
            attributes.add(new Attribute(lName, lValue));
        }
    }
    
    /**
     * Return the value for an attribute
     *
     * @param name  the attribute name
     * @return String   the attribute's value
     */
    public String getAttributeValue(String name) 
    {
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attr = attributes.get(i);
            if (name.equals(attr.name))
                return attr.value;
        }
        return null;
    }
    
    protected static String stripQuote(String input) 
    {
        if (input == null) return null;
        int index = input.indexOf('"');
        if (index >= 0) {
            int endIndex = input.indexOf('"', index + 1);
            if (endIndex >= 0) 
                return input.substring(index, endIndex);
        } 
        index = input.indexOf('\'');
        if (index >= 0) {
            int endIndex = input.indexOf('\'', index + 1);
            if (endIndex >= 0) 
                return input.substring(index, endIndex);
        } 
        return input;
    }
    
    /** @return tag name */
    public String getTagName() { return(tagName); }
    public int getEndLocation() { return endLocation; }

    public void setEndLocation(int endLocation) { this.endLocation =  endLocation; }

    /**
    * this method will return a String representation of this object
    *
    * @return String representation
    */
    public String toString()
    {
        StringBuffer sb = new StringBuffer( "HTML Tag=" );
        sb.append(tagName );
        for (int i = 0; i < attributes.size(); i++) {
            Attribute attr = attributes.get(i);
            sb.append(' ').append(attr.name);
            if (attr.value != null)
                sb.append('=').append(attr.value);
        }
        return sb.toString();
    }

    class Attribute 
    {
        private String name;
        private String value;
        
        Attribute(String name, String value) 
        {
            this.name = name;
            this.value = value;
        }
    }
}
