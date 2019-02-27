/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.dnsd;


/**
 * Represent the Header in a DnsdMessage.
 * @author Jing Zhou
 */
class DnsdHeader //extends com.brickstreet.common.lib.CRMObject
{
	/** id to match up queries and answers */
	private int	id;
	/** Authorative answer */
	private boolean	aa = true;
	/** Message truncated */
	private boolean	tc = false;
	/** Response code */
	private int	rCode = 0;
	/** Number of entries in the question section */
	private int	qdCount = 0;
	/** Number of resource records in the answer section */
	private int	anCount = 0;
	/** Number of resource records in the authority section */
	private int	nsCount = 0;
	/** Number of resource records in the additional section */
	private int	arCount = 0;

	/**
	 * Creates DnsdHeader with existing header.<br>
	 * This constructor should be used to create a response
	 * with the same ID of the incoming request.
	 * @param id ID for a pair of DNS request and response
	 */
	DnsdHeader(DnsdHeader header)
	{
		id = header.getId();
		qdCount = header.getqdCount();
	}

	/**
	 * Creates DnsdHeader with DnsdByteParser which helps to parse the binary data.
	 * @param parser parse out the Header from the byte array
	 */
	DnsdHeader(DnsdByteParser parser)
	{
		id = parser.readTwoByteInt();
		byte b = parser.readByte();
		aa = (b & 4) == 4;
		tc = (b & 2) == 2;
		rCode = parser.readByte() & 0xF;
		qdCount = parser.readTwoByteInt();
		anCount = parser.readTwoByteInt();
		nsCount = parser.readTwoByteInt();
		arCount = parser.readTwoByteInt();
	}

	/**
	 * Packs the Header data into Byte array for network transport.
	 * @param bytebuilder hold an byte array and help transfer data into binary format
	 */
	void pack(DnsdByteBuilder bytebuilder)
	{
		bytebuilder.writeTwoByteInt(id);

		// aa is the third bit and tc is second bit
		byte b = 0;
		if (aa)
			b |= 4;
		if (tc)
			b |= 2;
		bytebuilder.writeByte(b);
		
		// rCode
		bytebuilder.writeByte((byte)rCode);
		// QDCOUNT
		bytebuilder.writeTwoByteInt(qdCount);
		// ANCOUNT
		bytebuilder.writeTwoByteInt(anCount);
		// NSCOUNT
		bytebuilder.writeTwoByteInt(nsCount);
		// ARCOUNT
		bytebuilder.writeTwoByteInt(arCount);

	}
	
	/** Set the AnswerCount in the header */
	public void setAnswerCount(int count)
	{
		anCount = count;
	}
	
	/** Returns the question counter */
	public int getqdCount()
	{
		return qdCount;
	}

	/** Returns the message ID */
	public int getId()
	{
		return id;
	}

/*	Commented out for future use
	public boolean isAuthoritativeAnswer()
	{
		return aa;
	}

	public boolean isTrucated()
	{
		return tc;
	}
	
	public void setId(int id)
	{
		this.id =id;
	}

	public void setAuthanswer(boolean aa)
	{

	}

	public void setMtrucated(boolean tc)
	{

	}

	public void setResponseCode(int rcode)
	{

	}

	public void setqdCount(int qdcount)
	{

	}

	public void setnsCount(int nscount)
	{

	}

	public void setarCount(int arcount){

	}


	public int getId()
	{
		return id;
	}

	public boolean getAuthanswer()
	{
		return aa;
	}

	public boolean getMtrucated()
	{
		return tc;
	}

	public int getResponseCode()
	{
		return rCode;
	}

	public int nsCount()
	{
		return nsCount;
	}

	public int arCount()
	{
		return arCount;
	}
	*/
}