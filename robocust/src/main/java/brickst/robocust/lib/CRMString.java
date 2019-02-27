package brickst.robocust.lib;

import java.io.Serializable;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * a class contains a String and x position of this string
 * as a substring in another string	 <br>
 * This class is used to parse field from a target String <br>
 * <p>
 * a class used to process String object to 
 * extract smtp commond, parameter, data, ...
 */
public class CRMString implements Serializable
{
	static Logger logger = Logger.getLogger(CRMString.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2012665336165520277L;

	/** the string we represent. */
	protected String text = null;

	/** the beginning index of where this string was found in some other string;
	 * remembered so that our caller can determine where we came from in its
	 * original string (PreparedContent).
	 * @see CRMString#internalParseField */
	protected int	index=-1;

	/** the end index.
	 * @see CRMString#index */
	protected int   endIndex = -1;

    private int	leftMarkLength = -1;

    private int rightMarkLength = -1;

    public int getLeftMarkLength() {
        return leftMarkLength;
    }

    public int getRightMarkLength() {
        return rightMarkLength;
    }

	/**
	 * constructor, for serialization
	 */
    public CRMString() {super(); }
    
	/**
	 * constructor
	 */
	public CRMString(String text)
	{
		setText(text);	
	}

	/**
	 * constructor
	 */
	public CRMString(String text, int index, int endIndex)
	{
		setText(text);
		this.index = index;
		this.endIndex = endIndex;
	}

    /**
     * constructor
     */
    public CRMString(String text, int index, int endIndex, int leftMarkSize, int rightMarkSize)
    {
        setText(text);
        this.index = index;
        this.endIndex = endIndex;
        this.leftMarkLength = leftMarkSize;
        this.rightMarkLength = rightMarkSize;
    }

	/**
	 * this method will return the String object this extend holds
	 *
	 */
	public String getText()
	{
		return(text);
	}
	
	/**
	 * Set text for this CRMString
	 */
	protected void setText(String text) { this.text = text; }

	/**
	 * this method will return the length of the string
	 */
	public int length()
	{
		return((text==null)?0:text.length());
	}

	/**
	 * this method will return the starting position
	 */
	public int getPosition()
	{
		return(index);
	}

	/**
	 * this method will return the end position
	 */
	public int getEndPosition()
	{
		return(endIndex);
	}

	/* UNUSED
	 * this method will set index of this string as a substring
	 * in another string
	 *
	 * @param int	index
	private void setPosition(int index)
	{
		this.index = index;
	}
	 */

	/**
	 * this method will return a String representaion of this object
	 *
	 * @return String representation
	 */
	public String toString()
	{
		return(text+", location:" +index + ", endLocation: " + endIndex);
	}

    /**
     * this method will capitalizes the first letter of each 
     * "word" and lowercases everything else.
     *
     */
    public static final String capWords(String input)
    {
        if (input == null)
			return null;

        boolean toUpper = true;  // should next letter be upper cased
        int max = input.length();
        StringBuffer output = new StringBuffer(input);
        for (int i = 0; i < max; i++) {
            char ch = output.charAt(i);
            if (Character.isWhitespace(ch) || ch == '_') {
                toUpper = true;
            } else if (Character.isLetter(ch)) {
                if (toUpper) {
                    toUpper = false;
                    ch = Character.toUpperCase(ch);
                } else {
                    ch = Character.toLowerCase(ch);
                }
            }
            output.setCharAt(i, ch);
        }
        return(output.toString());
    }

	/** @return truncated string if it exceeds the limit; original string
	 * otherwise. */
	public static final String truncate(String st, int limit)
	{
		if (st == null)
			return(null);

		if (st.length() <= limit)
			return(st);

		return(st.substring(0, limit));
	}

    /**
	 * get data value
	 * 
	 * for example, <CODE>
	 * we receive text = "mail from: <jim>"
	 *  String name = CRMString(text, "<", ">") 
	 *  will strip 'jim' out of above message
	 *  </CODE>
	 *
	 * @param msg	input String
	 * @param leftMark	the beginning indication of a parameter 
	 * @param rightMark	the end indication of a parameter
     * @param leftMarkEnc	the encoded left indication of a parameter
     * @param rightMarkEnc	the encoded right side
	 *
	 * @return String	the substring in between leftMark and rightMark
	 */
	public static final String parseField(String msg, 
					String leftMark, String rightMark,
                    String leftMarkEnc, String rightMarkEnc
                    )
	{
		return (internalParseField(msg, leftMark, rightMark,
                                   leftMarkEnc, rightMarkEnc, 0, true)).getText();
	}

	/**
	 * Stripe all data appearanced in parameter indicator
	 *
	 * for example, we receive "send to: <jim> <david> <alex>"
	 * this routine will strip 'jim','david' and 'alex'
	 * out of above message and return as a VArray
	 *
	 * @param msg	input String
	 * @param leftMark	the left indication of a parameter 
	 * @param rightMark	the right side
     * @param leftMarkEnc	the encoded left indication of a parameter
     * @param rightMarkEnc	the encoded right side
	 *
	 * @return VArray 
	 */
	public static final ArrayList parseFields(String msg,
				String leftMark, String rightMark,
                String leftMarkEnc, String rightMarkEnc
                )
	{
		return internalParseFields(msg, leftMark, rightMark, leftMarkEnc, rightMarkEnc, true, false);
		//we don't care about position
	}

	/**
	 * Stripe all data appearanced in parameter indicator
	 *
	 * for example, we receive "send to: <jim> <david> <alex>"
	 * this routine will strip 'jim','david' and 'alex'
	 * out of above message and return as a VArray
	 *
	 * @param msg	input String
	 * @param leftMark	the left indication of a parameter 
	 * @param rightMark	the right side
     * @param leftMarkEnc	the encoded left indication of a parameter
     * @param rightMarkEnc	the encoded right side
	 *
	 * @return CRMStringExtend VArray of those fields and their locations
	 */
	public static final ArrayList parseFieldsLocation(String msg,
				String leftMark, String rightMark,
                String leftMarkEnc, String rightMarkEnc
                )
	{
		return internalParseFields(msg, leftMark, rightMark, leftMarkEnc, rightMarkEnc, true, true);
		// we care about position
	}

	/**
	 * This method will stripe value from a content even if leftMark 
	 * and right mark could be mixed together
	 *
	 * for example, we have "dear [FullName([FirstName],[LastName])]" <br>
	 * This routine will find "[FullName([FirstName],[LastName])]" 
	 * for you
	 *
	 * @param msg	input String
	 * @param leftMark	the left indication of a parameter 
	 * @param rightMark	the right side
	 *
	 * @return String	a value
	 */
	public static final String parseNestedField(String text,
				String leftMark, String rightMark)
	{
        if(text != null && text.length() > 0 ){
		char leftCh = leftMark.charAt(0);
		char rightCh = rightMark.charAt(0);
		int	cchText = text.length();
		int leftMarkSize = leftMark.length();
		int rightMarkSize = rightMark.length();
		int	ichMax  = cchText - rightMarkSize + 1;
		int begin = text.indexOf(leftMark);

		if (begin < 0) return null;

		begin += leftMarkSize;
		int count = 1;  // we already found a leftMark, let's find the
						// corresponding rightMark
		try {
			for (int i = begin; i < ichMax; i++)
				if (text.charAt(i) == leftCh) {
					if (text.substring(i, 
							i+leftMarkSize).equals(leftMark)) {
						// found another leftMark
						count++;
					}
				} else if (text.charAt(i) == rightCh) {
					if (text.substring(i, 
							i+rightMarkSize).equals(rightMark)) {
						// found another leftMark
						count--;
						if (count == 0) {
							// we found the rightMark that march our 
							// beginning leftMark
							return text.substring(begin, i);
						}
					}
				} 
		} catch (StringIndexOutOfBoundsException e) {
			// The exception is thrown if we hit end of string.
			logger.error(e.getMessage(), e);
		}
        }
		return null;
	}

	/**
	 * Stripe all data recursively appeared in parameter indicator
	 *
	 * for example, we have 
	 * "[FullName([FirstName],[LastName])], [Address]" <br>
	 * this method will return 
	 * "[FullName([FirstName],[LastName])]" and "[Address]"
	 *
	 * @param msg	input String
	 * @param leftMark	the left indication of a parameter 
	 * @param rightMark	the right side
	 *
	 * @return VArray	hold all values found 
	 */
	public static final ArrayList parseNestedFields(String msg,
				String leftMark, String rightMark)
	{
		ArrayList vector = new ArrayList();
		String lastOccurance = null;
		lastOccurance = parseNestedField(msg, leftMark, rightMark);
		String tmpData = msg;
		while (lastOccurance != null) {
			// only add to vector if length > 0, in case we have "<> <jim>"
			if (lastOccurance.length() > 0) 
				vector.add(lastOccurance);
			String tmp = leftMark+lastOccurance+rightMark;
			int index = tmpData.indexOf(tmp);
			if (index < 0) {
				//something wrong here, tmpData has to be exists,
				//since we just find it using parseField();
				logger.error("Could not find " + tmp);
			}
			tmpData = tmpData.substring( index + tmp.length());
			lastOccurance = parseNestedField(tmpData, leftMark, rightMark);
		}
		return vector;
	}

	/**
	 * Stripe all data recursively appeared in parameter indicator
	 *
	 * for example, we have 
	 * "[FullName([FirstName],[LastName])], [Address]" <br>
	 * this method will return 
	 * "FirstName"
	 * "LastName"
	 * "FullName([FirstName],[LastName])" 
	 * "Address"
	 *
	 * @param msg	input String
	 * @param leftMark	the left indication of a parameter 
	 * @param rightMark	the right side
	 *
	 * @return VArray	hold all values found 
	 */
	public static final ArrayList parseAllNestedFields(String msg,
				String leftMark, String rightMark)
	{
		ArrayList vector = new ArrayList();
		vector = parseNestedFields(msg, leftMark, rightMark);

		// note, we will not remove from vector, nor will we insert
		// we will only add new field at the end
		for (int i = vector.size(); i-- > 0;) {
			String field = (String) vector.get(i);
			ArrayList tmpVArray = parseAllNestedFields(field,
					leftMark, rightMark);

			// old code: vector.merge(tmpVArray);
			// This adds tmpVArray to end, filtering any objects that are already in vector
			// ArrayList has addAll() but this does not filter duplicates
			// Therefore we inline the logic here 
			// -CM 22May2010
			for (Object obj : tmpVArray) {
				if (! vector.contains(obj)) {
					vector.add(obj);
				}
			}
		}
		return vector;
	}

	/**
	 * Returns the index within the target string of the first occurrence of 
	 * the specified substring, ignore case. 
	 *
	 * @param text	the target string
	 * @param spec	the specified substring
	 *
	 * @return int	if the string argument occurs as a substring within 
	 *				the target string, then the index of the first character 
	 *				of the first such substring is returned; if it does 
	 *				not occur as a substring, -1 is returned. 
	 */
	public static int indexOfIgnoreCase(String text, String spec)
	{
		return indexOfIgnoreCase(text, spec, 0);
	}		

	/**
	 * Returns the index within the target string of the first occurrence of 
	 * the specified substring, starting at the specified index and ignore case.
	 *
	 * @param text	the target string
	 * @param spec	the specified substring
	 * @param fromIndex the begining index
	 *
	 * @return int	if the string argument occurs as a substring within 
	 *				the target string, then the index of the first character 
	 *				of the first such substring is returned; if it does 
	 *				not occur as a substring, -1 is returned. 
	 */
	public static int indexOfIgnoreCase(String text, String spec, int fromIndex)
	{
		String  specLC = spec.toLowerCase();
		int     specSize = spec.length();
		int     specIndex = 0;
		int		cchSpec = spec.length();
		int		ichMax  = text.length();
		for (int ich = fromIndex; ich < ichMax; ich++) {
			if (specIndex < specSize &&
			    Character.toLowerCase(text.charAt(ich)) == 
			        specLC.charAt(specIndex))
			    specIndex++;
			else 
			    specIndex = 0;
			
			if (specIndex == specSize) {
			    return (ich - specSize +1);
			}
		}

		return(-1);
	}		

	/**
	 * Returns the index within the target string of the first occurrence of 
	 * the specified substring that out of field, starting at the specified 
	 * index and ignore case. <pr>
	 * 
	 * For example: <code>
	 *     String a ="<a href=\"http://kana.com/<%[Name]%>.html\">";
	 *     String leftMark = "\"";
	 *     String rightMark = "\"";
	 *     int index = CRMString(a, ">", 0, "\"", "\"");
	 * </code> index should be 41. The last ">", not the ">" inside "\"".
	 *
	 * @param text	the target string
	 * @param spec	the specified substring
	 * @param fromIndex the begining index
	 * @param leftFieldMark 
	 * @param rightFieldMark 
	 *
	 * @return int	if the string argument occurs as a substring within 
	 *				the target string, then the index of the first character 
	 *				of the first such substring is returned; if it does 
	 *				not occur as a substring, -1 is returned. 
	 */
	public static int indexOutOfField(String text, 
	                                  String spec,
	                                  int fromIndex,
	                                  String leftFieldMark,
	                                  String rightFieldMark)
	{
		char	ch      = spec.charAt(0);
		int		ichMax  = text.length();
		int     specSize = spec.length();
		int     leftSize = leftFieldMark.length();
		int     rightSize = rightFieldMark.length();
		int     specIndex = 0;
		int     leftMarkIndex = 0;
		int     rightMarkIndex = 0;
		char    tmpChar;

		for (int ich = fromIndex; ich < ichMax; ich++) {
		    tmpChar = text.charAt(ich);
		    //we found left mark, we have to find right mark
		    if (leftMarkIndex == leftSize) {
		        if (rightMarkIndex < rightSize && 
		            tmpChar == rightFieldMark.charAt(rightMarkIndex)) {
		            rightMarkIndex++; 
		            if (rightMarkIndex == rightSize) { //we found right mark
		                rightMarkIndex =0;
		                leftMarkIndex = 0;
		                specIndex = 0;
		            }
		        } else {
		            rightMarkIndex = 0;
		        }
		    } else if (leftMarkIndex < leftSize && 
		        tmpChar == leftFieldMark.charAt(leftMarkIndex)) {
		            leftMarkIndex++;
		    } else if (text.charAt(ich) == spec.charAt(specIndex)) {
		    //we are outside a field
			    specIndex++;
			    if (specIndex == specSize)
			        return (ich - specSize +1);
			} else {
		        leftMarkIndex = 0;
			    specIndex = 0;
			}
	    }

		return(-1);
	}		

	/**
	 * parse ignore case
	 * 
	 * for example, we receive "mail from: <jim>"
	 * this routine will strip 'jim' out of above message
	 *
	 * @param msg	input String
	 * @param leftMark	the beginning indication of a parameter 
	 * @param rightMark	the end indication of a parameter
     * @param leftMarkEnc	the encoded left indication of a parameter
     * @param rightMarkEnc	the encoded right side
	 *
	 * @return String	the substring in between leftMark and rightMark
	 */
	public static final String parseFieldIgnoreCase(String msg, 
					String leftMark, String rightMark,
                    String leftMarkEnc, String rightMarkEnc
                    )
	{
		CRMString result = internalParseField(msg, leftMark, rightMark, leftMarkEnc, rightMarkEnc, 0, false);
		return((result==null) ? null : result.getText());
	}

	/**
	 * parse ignore case
	 *
	 * for example, we receive "send to: <jim> <david> <alex>"
	 * this routine will strip 'jim','david' and 'alex'
	 * out of above message and return as a array
	 *
	 * @param msg	input String
	 * @param leftMark	the left indication of a parameter 
	 * @param rightMark	the right side
     * @param leftMarkEnc	the encoded left indication of a parameter
     * @param rightMarkEnc	the encoded right side
	 *
	 * @return CRMStringExtend VArray of those fields and their locations
	 */
	public static final ArrayList parseFieldsIgnoreCase(String msg,
				String leftMark, String rightMark,
                String leftMarkEnc, String rightMarkEnc
                )
	{
		return internalParseFields(msg, leftMark, rightMark, leftMarkEnc, rightMarkEnc, false, false);
		// don't care about location
	}

	/**
	 * parse ignore case and return location
	 *
	 * for example, we receive "send to: <jim> <david> <alex>"
	 * this routine will strip 'jim','david' and 'alex'
	 * out of above message and return as a array
	 *
	 * @param msg	input String
	 * @param leftMark	the left indication of a parameter 
	 * @param rightMark	the right side
     * @param leftMarkEnc	the encoded left indication of a parameter
     * @param rightMarkEnc	the encoded right side
	 *
	 * @return CRMStringExtend VArray of those fields and their locations
	 */
	public static final ArrayList parseFieldsIgnoreCaseLocation(String msg,
				String leftMark, String rightMark,
                String leftMarkEnc, String rightMarkEnc
                )
	{
		return internalParseFields(msg, leftMark, rightMark, leftMarkEnc, rightMarkEnc, false, true);
		// we do care about location
	}

	/**
	 * this method will traverse a String and 
	 * replace the first occurrence of a particular subString with another
	 *
	 * @param source	source String
	 * @param target	the substring needs to be replaced
	 * @param replacewith	the new substring that replace the target
	 *
	 * @return String	result
	 */
	public static String replace(String source, String target,
			String replaceWith)
	{
		String result = source;
		int targetSize = target.length();
		int index = 0;
		if ((index = result.indexOf(target, index)) >= 0) {
			String leftSide = result.substring(0, index);
			String rightSide = result.substring(index + targetSize);
			result = leftSide + replaceWith + rightSide;
		}

		return result;
	}

	/**
	 * this method will traverse a Sting and 
	 * replace the all occurrence of a particular subString with another
	 *
	 * @param source	source String
	 * @param target	the substring needs to be replaced
	 * @param replaceWith	the new substring that replace the target
	 *
	 * @return String	result
	 */
	public static String replaceAll(String source, String target,
			String replaceWith)
	{
		String result = source;
		int targetSize = target.length();
		int index = 0;
		while ((index = result.indexOf(target, index)) >= 0) {
			String leftSide = result.substring(0, index);
			String rightSide = result.substring(index + targetSize);
			result = leftSide + replaceWith + rightSide;
		}

		return result;
	}

	/**
	 * private method, which used by all parseField method
	 *
	 * @param msg			input String
	 * @param leftMark		the left indication of a parameter 
	 * @param rightMark		the right side
     * @param leftMarkEnc	the encoded left indication of a parameter
     * @param rightMarkEnc	the encoded right side
	 * @param index         starting point
	 * @param caseSensative	a boolean value tells us whether we
	 *						should do case sensative or insensative
	 *						parse
	 *
	 * @return a CRMStringExtend contains the field and its location
	 */
	private static final CRMString internalParseField(String msg, 
				String leftMark, String rightMark, 
                String leftMarkEnc, String rightMarkEnc,
				int index, boolean caseSensative)
	{
		// make sure msg is not null
		if (msg == null) return null;

		int leftMarkSize = leftMark.length();
		int rightMarkSize = rightMark.length();

		int leftIndex = caseSensative ? msg.indexOf(leftMark, index)
									  : indexOfIgnoreCase(msg, leftMark, index);
		// if no leftIndex, not necessary to continue
		if (leftIndex < 0)
        {
            // let's try the encoded version of the mark
            if(     leftMarkEnc != null
                    && (leftMark.length() != leftMarkEnc.length() || !leftMark.equals(leftMarkEnc)))
            {
                leftIndex = caseSensative ? msg.indexOf(leftMarkEnc, index)
                                          : indexOfIgnoreCase(msg, leftMarkEnc, index);
                leftMarkSize = leftMarkEnc.length();
            }
            if(leftIndex < 0)
			    return null;
        }
		int beginIndex = leftIndex + leftMarkSize;
		int rightIndex = caseSensative
								? msg.indexOf(rightMark, beginIndex)
								: indexOfIgnoreCase(msg, rightMark, beginIndex);
		if (rightIndex <= beginIndex)
        {
            // let's try the encoded version of the mark
            if( rightMarkEnc != null
                && (rightMark.length() != rightMarkEnc.length() || !rightMark.equals(rightMarkEnc)))
            {
                rightIndex = caseSensative
                                        ? msg.indexOf(rightMarkEnc, beginIndex)
                                        : indexOfIgnoreCase(msg, rightMarkEnc, beginIndex);
                rightMarkSize = rightMarkEnc.length();
            }
            if (rightIndex <= beginIndex)
                return null;
        }

		return(new CRMString(msg.substring(beginIndex, rightIndex), 
							 beginIndex, rightIndex, leftMarkSize, rightMarkSize));
	}

	/**
	 * private method, used by all parseFields method
	 *
	 * @param msg	input String
	 * @param leftMark	the left indication of a parameter 
	 * @param rightMark	the right side
	 * @param caseSensative	a boolean value tells us whether we
	 *						should do case sensative or insensative
	 *						parse
	 * @param index			a boolean value tells us whether we
	 *						should return the location of those field
	 *						to you
	 *
	 * @return VArray 
	 */
	private static final ArrayList internalParseFields(String msg,
				String leftMark, String rightMark, 
                String leftMarkEnc, String rightMarkEnc,
				boolean caseSensative, boolean index)
	{
		ArrayList vector = new ArrayList();
		CRMString lastOccurance = internalParseField(msg, leftMark, rightMark,
                                                     leftMarkEnc, rightMarkEnc, 0, caseSensative);
		while (lastOccurance != null) {
			// only add to vector if length > 0, in case we have "<>"
			if (lastOccurance.length() > 0) {
				if (index) {  // we need location
					vector.add(lastOccurance);	// CRMString
				} else { // we only care about string
					vector.add(lastOccurance.getText());	// String
				}
			}

			int startIndex = lastOccurance.getEndPosition() + rightMark.length();
			lastOccurance = internalParseField(msg, leftMark, rightMark,
                                               leftMarkEnc, rightMarkEnc,  startIndex, caseSensative);
		}

		return vector;
	}

    /**
     * this method will parse field out of a string (text),
     * the field mark support abc.*xyz, means there maybe any 
     * number of white space/letters between abc and xyz.
     *
     * @param text   original content
     * @param left   abc in "abc.*xyz" format
     * @param left2  xyz in "abc.*xyz" format
     * @param right  rightMark of a field
     * @param begin  begin index where we start parsing
     */
	public static final CRMString parseField(
			String text, 
			String left,
			String left2,
			String right,
			int begin)
	{
		//in html, link should be something in between
		//"<a href=" and "</a>", or "<area...href=" and ">"

		// FUTURE: private static final constants: http, ftp, https.  Walk array
		//String left = "<a";
		//String leftArea = "<area";
		//String href = "href=";
		//String right = "</a>"; 
		//char rightArea = '>';
		//char httpLeftCh = '<';
		// it's impossible we have "ftp://EOF".  
		// REVIEW1: Explain.
		// REVIEWED: we search link from "begin" to text.length - https.length(),
		//          this means, if there's something "ftp://EOF" we will not be
		//          able to catch. However, we don't care about ftp:// since
		//          it is not a valid link any way. 
		int textLength = text.length();
		//int	iMax = textLength - leftArea.length() - href.length() - right.length();
		int leftSize = left.length();
		int left2Size = left2.length();
		int rightSize = right.length();
		int iMax = textLength - leftSize - left2Size - rightSize;
		//BitSet bitset = getBitSet(arraySize);
		
		//int hrefSize = href.length();
        
		char nextCh = ' ';
		//int currentIndex = 0; //the number of matched leftside
		for (int i = begin; i < iMax; i++) {
			if (Character.toLowerCase(text.charAt(i)) == 
				left.charAt(0) &&
				text.substring(i, i + leftSize).toLowerCase().
				equals(left)) {
				//found left, let's find left2
				// Determine first whitespace after http string.
				int iMax2 = iMax + leftSize;
				int i1 = i+ 1;
				for (; i1 < iMax2; i1++) {
					nextCh = Character.toLowerCase(text.charAt(i1));
					if (nextCh == left2.charAt(0) &&
				        text.substring(i1, 
				            i1+left2Size).toLowerCase().equals(left2)) {
				        // we found left2=, now let's search for end right
				        int beginIndex = i1 + left2Size;
					    int j = beginIndex;
					    int iMax3 = iMax2 + left2Size;
					    for (; j < iMax3; j++) {
				        	if (text.substring(j, j + rightSize).
				        		toLowerCase().equals(right)) {
					            return(new CRMString(
					                    text.substring(beginIndex, j), 
					                            beginIndex, j));
					        }
					    }
					} else if (
				        text.substring(i1, 
				            i1+rightSize).toLowerCase().equals(right)) {
					    //if we found '>' before 'href=', break
					    break;
					} else {
					    continue;
					}
				} 						
			 } 
		} 
		return null;
	} 
	
	public static final CRMString parseField(
			String text, 
			String left1,
			String left2,
			String right,
			int begin,
			String leftFieldMark,
			String rightFieldMark)
	{
		//in html, link should be something in between
		//"<a href=" and "</a>", or "<area...href=" and ">"

		// FUTURE: private static final constants: http, ftp, https.  Walk array
		// it's impossible we have "ftp://EOF".  
		// REVIEW1: Explain.
		// REVIEWED: we search link from "begin" to text.length - https.length(),
		//          this means, if there's something "ftp://EOF" we will not be
		//          able to catch. However, we don't care about ftp:// since
		//          it is not a valid link any way. 
		int textLength = text.length();
		int left1Length = left1.length();
		int left2Length = left2.length();
		int rightLength = right.length();
        
		int left1Index = 0;
		int left2Index = 0;
		int rightIndex = 0;
		for (int i = begin; i < textLength; i++) {
			// FUTURE: Abstract these if clauses!
			// FUTURE: Use case insensitive search function in CRMString.
			if (left1Index < left1Length &&
			        Character.toLowerCase(text.charAt(i)) ==
			        left1.charAt(left1Index)) {
			    left1Index++;
			} else if ( left1Index >= left1Length && 
			        left2Index < left2Length &&
			        Character.toLowerCase(text.charAt(i)) ==
			        left2.charAt(left2Index)) {
			     left2Index++;
			     if (left2Index >= left2Length ) {
			        int beginIndex = i + 1;
			        //now we found left1, left2, let's find right
			        int endIndex = CRMString.indexOutOfField(text, 
			                        right, beginIndex, 
			                        leftFieldMark, rightFieldMark);
			        if (endIndex > beginIndex) {
				        return(new CRMString(
					            text.substring(beginIndex, endIndex), 
					            beginIndex, endIndex));
			        } else {
			            return null;
			        }
			     }
		    } else if ( rightIndex < rightLength &&
			        Character.toLowerCase(text.charAt(i)) ==
			        left2.charAt(left2Index)) {
			     rightIndex++;
			     //if we found rightIndex before we found left2 index
			     //restore the index
			     if (rightIndex == rightLength &&
			            left2Index < left2Length ) {
			         left1Index = 0;
			         left2Index = 0;
			         rightIndex = 0;
			     } else {  
			        assert(false); //impossible, covered in case above
			     }
		    } else {
			    if (left1Index < left1Length) 
			        left1Index = 0;
			    left2Index = 0;
			    rightIndex = 0;
		    }
		            
		}
		return null;
	} 
}
