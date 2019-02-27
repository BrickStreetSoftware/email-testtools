/*
 * @(#)DotStuffOutputStream.java 2000-01-26
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 *
 */

package brickst.robocust.smtp;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This output stream class does dot stuffing.
 * It converts the sequence CRLF. to CRLF.. so that a lone dot inside a message body
 * doesn't signal SMTP end-of-message by mistake.
 * It also translates and lone CR and LF to CRLF.
 * Used by SmtpSender.
 *
 * @see   com.kana.connect.common.net.smtp.SmtpSender
 *
 * @author  CJ Lofstedt 01/24/00
 *
 * @deprecated use class from messageengine/commonlib
 */
class SmtpOutputStream extends FilterOutputStream
{
	/** Previous character, no previous character should be treated as LF */
	private int	prev = '\n';

    /**
     * Creates a new output stream for dot stuffing
     * and line break translation.
     *
     * @param   out    the underlying output stream.
     */
    public SmtpOutputStream(OutputStream out)
    {
		super(out);
    }

	/**
	 * Translates LF. to LF.. and lone CR and LF to CRLF.
	 * See OutputStream for parameters and return values.
	 */
    public void write(int b) throws IOException
    {
		// Add CR to lone LF
		if (b == '\n' && prev != '\r')
		{
			out.write('\r');
			prev = '\r';
		}
		// Add LF to lone CR
		else if (prev == '\r' && b != '\n')
		{
			out.write('\n');
			prev = '\n';
		}

		// Dot stuff LF.
		if (prev == '\n' && b == '.')
		{
		    out.write('.');
		}
		out.write(b);
		prev = b;
    }

	/**
	 * Translates LF. to LF.. and lone CR and LF to CRLF.
	 * See OutputStream for parameters and return values.
	 */
    public void write(byte b[], int off, int len) throws IOException
    {
		int endOffset = len + off;

		for (int i = off; i < endOffset; i++)
		{
			write(b[i]);
		}

		/* Unnecessary optimization
		len += off;

		for (int i = off; i < len; i++)
		{
			// Add CR before lone LF
			if (b[i] == '\n' && prev != '\r')
			{
				out.write(b, off, i - off);
				off = i;
				out.write('\r');
				prev = '\r';
			}
			// Add LF after lone CR
			else if (prev == '\r' && b[i] != '\n')
			{
				out.write(b, off, i - off);
				off = i;
				out.write('\n');
				prev = '\n';
			}

			// Dot stuff .LF
	    	if (prev == '\n' && b[i] == '.')
	    	{
				out.write(b, off, i - off);
				off = i;
				out.write('.');
	    	}
		    prev = b[i];
		}
		if ((len - off) > 0)
		    out.write(b, off, len - off);
		*/
    }
}
