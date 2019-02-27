/*
 * @(#)AsciiString.java 2000/01/24
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 * 
 */

package brickst.robocust.smtp;

/**
 * This class encapsulates an ASCII string. (We use the word ASCII string a
 * bit loose here, it could also be an ISO-8859-1 string. US-ASCII is a 
 * subset of ISO-8859-1 is a subset of Unicode.)
 * The content is stored as bytes, so conversion to on the wire format
 * is much faster than String. (Since String needs to do character set 
 * conversions.)
 * It will grow as needed.
 *
 * @author  CJ Lofstedt 01/24/2000
 *
 */
public class AsciiString 
{
	/** The stored ascii string */
    private byte value[];

	/** The length of the array */
    private int length;

	/** The number of characters stored */
    private int count;

    /**
     * Constructor with default (16) size.
     */
	public AsciiString()
	{
		this(16);
	}

    /**
     * Constructor
	 *
     * @param length the initial size of the string
     */
	public AsciiString(int length)
	{
		count = 0;
		value = new byte[length];
		this.length = length;
	}

    /**
     * Clears the string, i.e. sets its length to zero.
     */
    public void clear()
    {
		count = 0;
	}

    /**
     * Returns the length of this AsciiString.
     *
     * @return  the number of ASCII characters.
     */
    public int length()
    {
		return count;
    }

    /**
     * Sets the length of this AsciiString.
	 * If the new length is longer than the current length, the AsciiString
	 * will grow and get filled with null characters/bytes.
	 * If the new length is shorter than the current length, the AsciiString
	 * is truncated to the new length.
	 *
     * @param newLength the new length of this AsciiString
     */
    public void setLength(int newLength)
    {
		// Grow the array as needed
		if (newLength > length)
	    	grow(newLength);

		count = newLength;
    }

    /**
     * Returns the character at index position.
     *
     * @param index the position of the character to return
     * @return  the returned ASCII character
     */
	public char charAt(int index)
	{
		if (index >= count)
			throw new ArrayIndexOutOfBoundsException();
		return (char)(value[index] & 0xFF);
	}

    /**
     * Returns the byte at index position.
     *
     * @param index the position of the character to return
     * @return  the returned byte character
     */
	public byte byteAt(int index)
	{
		if (index >= count)
			throw new ArrayIndexOutOfBoundsException();
		return value[index];
	}

    /**
     * Sets this strings value to that of the argument String object.
     *
     * @param str the String that will overwrite the contents of this string
     * @return  this object
     */
    public AsciiString set(String str)
    {
		count = 0;
		return append(str);
	}

    /**
     * Appends a String object.
     *
     * @param str the String to append
     * @return  this object
     */
    public AsciiString append(String str)
    {
		int len = str.length();
		int newcount = count + len;
		if (newcount > length)
	    	grow(newcount);

		// Assume we only have ASCII (8bit) characters
		for (int i=0; i < len; i++)
			value[count++] = (byte)str.charAt(i);

		return this;
	}

    /**
     * Appends an AsciiString object.
     *
     * @param str the AsciiString to append
     * @return  this object
     */
    public AsciiString append(AsciiString asciiString)
    {
		int len = asciiString.length();
		int newcount = count + len;
		if (newcount > length)
	    	grow(newcount);

		System.arraycopy(asciiString.getValue(), 0, value, count, len);
		count = newcount;

		return this;
	}

    /**
     * Appends a character.
     *
     * @param c the character to append
     * @return  this object
     */
    public AsciiString append(char c)
    {
		if (count == length)
			grow(count+1);
		value[count++] = (byte)c;
		return this;
	}

    /**
     * Appends an ASCII character as a byte.
     *
     * @param b the ASCII character to append
     * @return  this object
     */
    public AsciiString append(byte b)
    {
		if (count == length)
			grow(count+1);
		value[count++] = b;
		return this;
	}

    /**
     * Appends a carrige return line feed sequence (CRLF).
     *
     * @return  this object
     */
    public AsciiString appendCRLF()
    {
		if ((count+2) > length)
			grow(count+2);
		value[count++] = 13;
		value[count++] = 10;
		return this;
	}

    /**
     * Converts this object to a String object.
     * Assumes all characters are US-ASCII or Latin1.
	 *
     * @return  String object
     */
	public String toString()
	{
		// We only have ASCII or Latin1 (8bit) characters
		char[] chars = new char[count];
		for (int i=0; i<count; i++)
			chars[i] = (char)(value[i] & 0xFF);

    	return new String(chars);
	}

    /**
     * Converts this object to a String object.
     *
     * @param charset the character set to use for the conversion.
     * @return  String object
     */
	public String toString(String charset) 
		throws java.io.UnsupportedEncodingException
	{
    	return new String(value, 0, count, charset);
	}

    /**
     * Returns a byte array representing this ASCII string.
     *
     * @return  byte array
     */
	byte[] getBytes()
	{
		byte[] bytes = new byte[count];
		System.arraycopy(value, 0, bytes, 0, count);
		return bytes;
	}

    /**
     * Grows the byte array.
     *
     * @param minimumCapacity the new minimum size
     */
	private void grow(int minimumCapacity)
	{
		int newCapacity = (length + 1) * 2;
		if (minimumCapacity > newCapacity)
	    	newCapacity = minimumCapacity;
		byte newValue[] = new byte[newCapacity];
		System.arraycopy(value, 0, newValue, 0, count);
		value = newValue;
		length = newCapacity;
	}

    /**
     * Returns the actual byte array.
     * Only for use by other objects of the same type.
     *
     * @return  the value byte array
     */
	private byte[] getValue()
	{
		return value;
	}

	/**
	 * Writes the AsciiString to the OutputStream out.
     *
     * @param out the OutputStream to write the AsciiString to
	 */
	public void writeTo(java.io.OutputStream out) throws java.io.IOException
	{
		out.write(value, 0, count);
	}
}
