/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.dnsd;


/**
 * The question section of a DNS packet.<br>
 * The DNS client will set up the question.<br>
 * The DNS handler in the server will parse the question part for the type of query.
 *
 * @author Jing Zhou
 * @version 0.1
 */
class DnsdQuestion //extends com.brickstreet.common.lib.CRMObject
{
	/** domain name */
	private String qName;
	/** question type*/
	private int qType;
	/** class of question(always be internet = 1)*/
	private	int qClass = 1;

	/**
	 * Create DnsdQuestion with DnsdByteParser which helps to parse the binary data.
	 * @param parser parse out the Header from the byte array
	 */
	 DnsdQuestion(DnsdQuestion question)
	 {
		qName = question.getName();
		qType = question.getType();
	 }

	/**
	 * Create DnsdQuestion with DnsdByteParser which helps to parse the binary data.
	 * @param parser parse out the Header from the byte array
	 */
	DnsdQuestion(DnsdByteParser parser)
	{
		qName = parser.readName();
		qType = parser.readTwoByteInt();
		qClass = parser.readTwoByteInt();
	}


	/**
	 * Pack the Question into Byte array for network transport.
	 * @param bytebuilder hold an byte array and help transfer data into binary format
	 */
	void pack(DnsdByteBuilder bytebuilder)
	{
		bytebuilder.writeName(qName);
		bytebuilder.writeTwoByteInt(qType);
		bytebuilder.writeTwoByteInt(qClass);
	}


	public String getName()
	{
		return qName;
	}

	public int getType()
	{
		return qType;
	}
/*
	should never be named as getClass
	public int getqClass()
	{
		return qClass;
	}
*/
   /*
	public void setqName(String qname)
	{
	}

	public void setqType(int qtype)
	{
	}

	public void setqClass(int qclass)
	{
	}
	*/
}