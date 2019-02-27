/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.dnsd;

/**
 * A util class help covert int and String into byte array.
 * It encodes the DNS binary data.
 * @author Jing Zhou
 */
class DnsdByteBuilder //extends com.brickstreet.common.lib.CRMObject
{
	/** hold the byte array to be built */
	private byte[] array;
	/** maximum length for data in a udp packet */
	private final int MAX_LENGTH = 512;
	
	/** 
	 * The pointer of the current position in the array. 
	 * Offset is the actual length of the data when the pack finished.
	 */	
	private int	offset;

	/**
	 * Creates DnsByteBuilder
	 * Initial the array with max length
	 */
	DnsdByteBuilder()
	{
		array = new byte[MAX_LENGTH];
		offset =0;
	}
	 
	/** Returns the encoded byte array */ 
	byte[] getBytes()
	{
		return array;
	}
	 
	/** Returns the current position */
	int getCurrentPosition()
	{
		return offset;
	}

	/**
	 * Skips i bytes in the DnsByteBuilder and
	 * advances the current position.
	 *
     * @param i the number of byte positions to advance
	 */
	public void skip(int i)
	{
		offset += i;
	}	
	
	/** Appends a byte to the array. */
	void writeByte(byte b)
	{
		array[offset++] = b; 
	}
	
	/** Appends a int of two bytes to the array. */
	void writeTwoByteInt(int value)
	{
		array[offset++] = (byte)(value>>8);
		array[offset++] = (byte)(value & 0xFF);
	}
	
	/** Appends a int of four bytes to the array. */
	void writeFourByteInt(int value)
	{
		array[offset++] = (byte)((value>>24) & 0xFF);
		array[offset++] = (byte)((value>>16) & 0xFF);
		array[offset++] = (byte)((value>>8) & 0xFF);
		array[offset++] = (byte)(value & 0xFF);
	}
	
	/**
	 * Sets two bytes in the array at certain position.
	 * @param value the integer to be inserted
	 * @param position the position in the byte array to set these two byte
	 */
	void setTwoByteInt(int value, int position)
	{
		array[position++] = (byte)(value>>8);
		array[position++] = (byte)(value & 0xFF);
	}
	
	/**
	 * Encodes a domain name as a sequence of labels.
	 * @param name the domain name to encode
	 */
	void writeName(String name)
	{ 
		int dotIndex;
		int endIndex;
		int fromIndex = 0;
		
		// For each label
		do {
			dotIndex = name.indexOf('.', fromIndex);
			if (dotIndex == -1)
				endIndex = name.length();
			else
				endIndex = dotIndex;
			// Store the label length
			array[offset++] = (byte)(endIndex - fromIndex);
			
			// Store the label characters
			while (fromIndex < endIndex)
				array[offset++] = (byte)name.charAt(fromIndex++);
			    fromIndex = endIndex + 1;
		} while (dotIndex != -1);

		// Add the terminating 0 length label
		array[offset++] = 0;
	}	
}
