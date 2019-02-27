/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.dnsd;

/**
 * MX Record answers where is the mail server: e.g mail.kana.com.
 *
 * @author Jing Zhou
 * @version 0.1
 * @see DnsdResponse
 */
public class DnsdMXRecord extends DnsdRecord
{
    /** preference value for this mail host */
	private int preference;

	/** mail exchange domain name string */
	private String mailExchange;

	/**
	 * Creates a DNS MX type Record.
	 *
	 * @param name the domain name (kana.com)
	 * @param ttl time to live: how long the record should be valid in seconds
	 * @param preference preference value for this mail host
	 * @param mailExchange mail exchange domain name string
	 */
	public DnsdMXRecord(String name, int ttl, int preference, String mailExchange)
	{
		super(name, TYPE_MX, ttl);
		this.preference = preference;
		this.mailExchange = mailExchange;
	}

	/**
	 * Pack up MX Record into Byte Array.
	 * @param bytebuilder hold a byte array and help transfer data into binary format
	 */
	void pack(DnsdByteBuilder bytebuilder)
	{
		super.pack(bytebuilder);
		
		//take down the position for the length
		int position = bytebuilder.getCurrentPosition();

		//skip the length
		bytebuilder.skip(2);

		bytebuilder.writeTwoByteInt(preference);
		bytebuilder.writeName(mailExchange);
		int length = bytebuilder.getCurrentPosition() - position;
		
		// bugfix: don't count length bytes in length
		length -= 2;
		
		//go back set the length
		bytebuilder.setTwoByteInt(length, position);
	}
}