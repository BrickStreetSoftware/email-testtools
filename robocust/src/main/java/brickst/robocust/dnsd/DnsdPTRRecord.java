/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.dnsd;

/**
 * DnsdPTRRecord holds a domain name pointer (the answer to reverse DNS lookup)
 * @author CJ Lofstedt
 * @see DnsdResponse
 * @see DnsdRecord
 */
public class DnsdPTRRecord extends DnsdRecord
{
	/** Domain name pointer */
	private String ptrDname;

	/**
	 * Creates a DNS PTR type Record.
	 *
	 * @param name the domain name (kana.com)
	 * @param ttl time to live how long the record should be valid in seconds
	 * @param prtDname the domain name pointer
	 */
	public DnsdPTRRecord(String name, int ttl, String ptrDname)
	{
		super(name, TYPE_PTR, ttl);
		this.ptrDname = ptrDname;
	}

	/**
	 * Packs this record
	 * @param bytebuilder hold a byte array and help transfer data into binary format
	 */
	void pack(DnsdByteBuilder bytebuilder)
	{
		super.pack(bytebuilder);

		// Save the current position (for the length)
		int position = bytebuilder.getCurrentPosition();

		//skip the length
		bytebuilder.skip(2);

		// Write the domain name pointer
		bytebuilder.writeName(ptrDname);
		int length = bytebuilder.getCurrentPosition() - position;
		
		// Go back and set the length
		bytebuilder.setTwoByteInt(length, position);
	}
}