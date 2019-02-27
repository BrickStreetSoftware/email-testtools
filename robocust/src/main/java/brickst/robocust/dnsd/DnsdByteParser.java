/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.dnsd;


/**
 * A util class help read data (e.g. int) from the byte array.
 * It decodes DNS binary data.
 * @author C.J, Jing Zhou
 */
class DnsdByteParser //extends com.brickstreet.common.lib.CRMObject
{
	/** hold the byte array to be parsed */
	private byte[]	array;
	/** the actual length of data in the array */
	private int		length;
	/** the current position of reading */
	private int		offset;

	StringBuffer name;
	
	/**
	 * Creates a DnsdByteParser.
	 * @param array a byte array holds the binary data of a DNS Request.
	 * @param length the length of actual data in that array.
	 */
	DnsdByteParser(byte[] array, int length)
	{
		this.array = array;
		this.length = length;
		offset = 0;
	}

	/**
	 * If we are past the end, i.e. the offset
	 * has passed the length of the DnsByteArray.
	 */
	public boolean isPastEnd()
	{
		return offset > length;
	}

	/**
	 * Skips i bytes in the DnsByteArray and
	 * advances the current position.
	 *
	 * @param i the number of byte positions to advance
	 */
	public void skip(int i)
	{
		offset += i;
	}

	/**
	 * Reads the next byte from the array
	 */
	public byte readByte()
	{
		return array[offset++];
	}

	/**
	 * Reads a 16-bit (2-byte) integer from the array
	 */
	public int readTwoByteInt()
	{
		return ((array[offset++] & 0xFF) << 8) |
				(array[offset++] & 0xFF);
	}

	/**
	 * Reads a 32-bit (4-byte) integer from the array
	 */
	public int readFourByteInt()
	{
		return ((array[offset++] & 0xFF) << 24) |
				((array[offset++] & 0xFF) << 16) |
				((array[offset++] & 0xFF) << 8) |
				(array[offset++] & 0xFF);
	}

	/**
	 * Reads a domain name (sequence of labels) from the array and does
	 * all necessary decoding and decompression.
	 */
	public String readName()
	{
		if (name == null)
			name = new StringBuffer();
		else
			name.setLength(0);

		int nextOffset = 0;
		int labelLen = array[offset++] & 0xFF;

		// For each label... (End of domain name is marked by a zero-length label.)
		while (labelLen != 0)
		{
			// Is this a pointer (compressed label) ?
			if ((labelLen & 0xC0) == 0xC0)
			{
				int pointer = ((labelLen & 0x3F) << 8) | (array[offset++] & 0xFF);
				// Save the offset if this is the first time we find a pointer
				if (nextOffset == 0)
					nextOffset = offset;
				// The new offset is the pointer
				offset = pointer;
				labelLen = array[offset++] & 0xFF;
			}
			// Decode label
			else
			{
				labelLen &= 0x3F;

				// Add the label characters to the StringBuffer
				while (labelLen > 0)
				{
					name.append((char)array[offset++]);
					labelLen--;
				}

				labelLen = array[offset++] & 0xFF;
				// Append dot if this isn't the last label
				if (labelLen != 0)
					name.append('.');
			}
		}

		// Restore offset in case we got a pointer earlier
		if (nextOffset != 0)
			offset = nextOffset;

		return name.toString();
	}
}