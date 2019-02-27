/*
 * @(#)HtmlForm.java
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import java.util.*;
import brickst.robocust.lib.*;

/**
 * HtmlFormInput is an object to hold data of "input" tag of an HTML "form".
 *
 * @author Jim Mei
 */
public class HtmlForm extends HtmlBaseTag
{
    public static final String ACTION = "ACTION";
    public static final String METHOD = "METHOD";
    
    private ArrayList<HtmlFormInput> formInputs;
        
    /**
    * constructor
    *
    * @param   url
    */
    private HtmlForm(CRMString form)
    {
        super(form);
    }

    protected void parseValue(String form)
    {
        super.parseValue(form);
        
        formInputs = new ArrayList<HtmlFormInput>();
        
        //find all formInput
        int beginIndex = 0;
        String formLowerCase = form.toLowerCase();
        HtmlFormInput input = HtmlFormInput.parseFormInput(form, formLowerCase, beginIndex);
        while ((input = HtmlFormInput.parseFormInput(form, formLowerCase, beginIndex)) != null)
        {
            formInputs.add(input);
            beginIndex = input.getEndLocation();
        }
    }
    
    /** @return link name */
    public String getAction() { return getAttributeValue(ACTION); }
    /** @return value */
    public String getMethod() { return getAttributeValue(METHOD); }
    public ArrayList<HtmlFormInput> getFormInputs() { return formInputs; }

    public static HtmlForm parseFormInput(String content) 
    {
        return (parseForm(content, 0));   
    }
    
    //for performance
    public static HtmlForm parseForm(String content, String contentLower) 
    {
        return (parseForm(content, contentLower, 0));   
    }

    public static HtmlForm parseForm(String content, int beginIndex) 
    {
        return parseForm(content, content.toLowerCase(), beginIndex);   
    }

    public static HtmlForm parseForm(String content, String contentLower, int beginIndex) 
    {
        CRMString rawResult = parseRawResult(content, contentLower, beginIndex);   
        if (rawResult == null) return null;
        
        return new HtmlForm(rawResult);
    }


    private static String startTag = "<form";
    private static String endTag = "</form>";
    
    /**
    * This method will stripe URL from a html content 
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
            index = index + startTag.length();
            int endIndex = textLowerCase.indexOf(endTag, index);
            if (endIndex >= 0) 
                return new CRMString( text.substring(index, endIndex), 
	                        index, endIndex);
        }
        return null;
    } 

}
