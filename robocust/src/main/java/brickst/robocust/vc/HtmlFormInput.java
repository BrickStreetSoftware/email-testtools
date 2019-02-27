/*
 * @(#)HtmlFormInput.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;

/**
 * HtmlFormInput is an object to hold data of "input" tag of an HTML "form".
 *
 * @author Jim Mei
 */
public class HtmlFormInput extends HtmlBaseTag
{
    public static final String NAME = "NAME";
    public static final String TYPE = "TYPE";
    public static final String VALUE = "VALUE";
    
    /**
    * constructor
    *
    * @param   url
    */
    private HtmlFormInput(CRMString formInput)
    {
        super(formInput);
    }

    public static HtmlFormInput parseFormInput(String content) 
    {
        return (parseFormInput(content, 0));   
    }
    
    //for performance
    public static HtmlFormInput parseFormInput(String content, String contentLower) 
    {
        return (parseFormInput(content, contentLower, 0));   
    }

    public static HtmlFormInput parseFormInput(String content, int beginIndex) 
    {
        return parseFormInput(content, content.toLowerCase(), beginIndex);   
    }

    public static HtmlFormInput parseFormInput(String content, String contentLower, int beginIndex) 
    {
        CRMString rawResult = parseRawResult(content, contentLower, beginIndex);   
        if (rawResult == null) return null;
        
        return new HtmlFormInput(rawResult);
    }


    private static String startTag = "<input";
    private static String endTag = ">";
    
    /**
    *
    * @param text	input String
    * @param textLowerCase lowercased input String
    * @param int	begin point
    *
    * @return a CRMString
    */
    private static final CRMString parseRawResult(String text, 
                                                String textLowerCase,
                                                int begin)
    {
        int index = textLowerCase.indexOf(startTag, begin);
        if (index >= 0) {
            int endIndex = textLowerCase.indexOf(endTag, index + startTag.length());
            if (endIndex >= 0) 
                return new CRMString( text.substring(index, endIndex), 
	                        index, endIndex);
        }
        return null;
    } 

}
