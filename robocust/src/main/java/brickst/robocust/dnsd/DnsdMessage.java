/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.dnsd;


/**
 * Represent the Header and the Questions which are both in the DNS request and
 * DNS response.
 *
 * @author Jing Zhou
 */
abstract class DnsdMessage //extends com.brickstreet.common.lib.CRMObject
{
	/** Header is of fixed length of 12 bytes. */
	private DnsdHeader header;

	/** Client's query info, only one question in most case. */
	private DnsdQuestion[] questions;

	/**
	 * Create DnsdMessage with an ID.
	 * This constructor should be used to create a response
	 * with the same ID of the incoming request.
	 * @param parser parse out the Message from the byte array
	 */
	DnsdMessage(DnsdHeader header, DnsdQuestion[] questions)
	{
		this.header = new DnsdHeader(header);

		this.questions = new DnsdQuestion[questions.length];
		for(int i = 0; i < questions.length; i++) {
			this.questions[i] = new DnsdQuestion(questions[i]);
		}

	}

	/**
	 * Create DnsdMessage with DnsdByteParser which helps to parse the binary data.
	 * @param parser parse out the Message from the byte array
	 */
	DnsdMessage(DnsdByteParser parser)
	{
		header = new DnsdHeader(parser);
		int qCount = header.getqdCount();
		questions = new DnsdQuestion[qCount];
		for( int i = 0; i < qCount; i++) {
			questions[i] = new DnsdQuestion(parser);
		}
	}

	/**
	 * Pack up Message into Byte Array.
	 * @param bytebuilder hold a byte array and help transfer data into binary format
	 */
	void pack(DnsdByteBuilder bytebuilder)
	{
		header.pack(bytebuilder);
		for(int i = 0; i < questions.length; i++) {
			questions[i].pack(bytebuilder);
		}
	}

	/** Returns the header of the message */
	DnsdHeader getHeader()
	{
		return header;
	}

	/** Returns the questions of the message */
	DnsdQuestion[] getQuestions()
	{
		return questions;
	}

	/** Returns the name field in the first question */
	public String getName()
	{
		return questions[0].getName();
	}

	/** Returns the type field in the first question */
	public int getType()
	{
		return questions[0].getType();
	}
}
