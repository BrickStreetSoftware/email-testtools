/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.dnsd;

/**
 * Represent Record in DnsdResponse.<br>
 * It has variable length of data.<br>
 * A Record could be several types: A, MX, CNAME, and PTR.
 *
 * @author Jing Zhou
 * @see DnsdResponse
 */
public abstract class DnsdRecord //extends com.brickstreet.common.lib.CRMObject
{
	/** A host address */
	public static final int TYPE_A = 1;
	/** Identifies the canonical name of an alias */
	public static final int TYPE_CNAME = 5;
	/** Domain name pointer */
	public static final int TYPE_PTR = 12;
	/** Identifies a mail exchange for the domain */
	public static final int TYPE_MX = 15;

	/** domain name */
	private	String	name;
	/** record Type */
	private	int		type;
	/** record class (always be 1 which is internet) */
	private	final int rrClass=1;
	/** time to live: how long the record should be valid in seconds */
	private	int		ttl;
	
	/* length of the data in the record data field */
	//private int		rdLength;

	/**
	 * Creates a DNS A type Record
	 * @param name the domain name (kana.com)
	 * @param type the type of the record (A, MX ...)
	 * @param ttl time to live: how long the record should be valid in seconds
	 */
	public DnsdRecord(String name, int type, int ttl)
	{
		this.name = name;
		this.type = type;
		this.ttl = ttl;
	}
	
	/**
	 * Packs up a Record into Byte Array
	 * @param bytebuilder hold a byte array and help transfer data into binary format
	 */
	 void pack(DnsdByteBuilder bytebuilder)
	 {
		bytebuilder.writeName(name);
		bytebuilder.writeTwoByteInt(type);
		bytebuilder.writeTwoByteInt(rrClass);
		bytebuilder.writeFourByteInt(ttl);

		//length is left for the ARecord or MXRecord to write
	}
}