package brickst.robocust.lib;

import org.apache.log4j.Logger;

/**
 * MessageContext embodies the context of a message.<p>
 *
 * It contains three identifying pieces of information:<br>
 *      <li>Customer ID
 *		<li>Instance ID
 *		<li>Event Queue ID
 */
public class MessageContext {
	static Logger logger = Logger.getLogger(MessageContext.class);
	private long customer_id;
	private long instance_id;
	private long event_queue_id;
	private String senderPrefix;
	private String senderDomain;
	
	//
	// GETTERS / SETTERS
	//
	
	public long getCustomerId() {
		return customer_id;
	}
	public void setCustomerId(long customerId) {
		customer_id = customerId;
	}
	public long getInstanceId() {
		return instance_id;
	}
	public void setInstanceId(long instanceId) {
		instance_id = instanceId;
	}
	public long getEventQueueId() {
		return event_queue_id;
	}
	public void setEventQueueId(long eventQueueId) {
		event_queue_id = eventQueueId;
	}
	public String getSenderPrefix() {
		return senderPrefix;
	}
	public void setSenderPrefix(String senderPrefix) {
		this.senderPrefix = senderPrefix;
	}
	public String getSenderDomain() {
		return senderDomain;
	}
	public void setSenderDomain(String senderDomain) {
		this.senderDomain = senderDomain;
	}
	
	//
	// CONSTRUCTORS
	//
	
	public MessageContext()
	{
	}
	
	public MessageContext(String addr)
	{
		if (! parseContextFromString(addr)) {
			throw new IllegalArgumentException("Invalid: " + addr);
		}
	}
	
	/** 
	 * parses address, return message context if valid, else null
	 * @param addr
	 * @return MessageContext or null
	 */
	public static MessageContext parseMessageContext(String addr)
	{
		MessageContext mc = new MessageContext();
		if (mc.parseContextFromString(addr)) {
			return mc;
		}
		else {
			return null;
		}
	}
	
    /**
     * Parses out the contextual tracking information from the To: field
     * of the message.
     *
     * The expected format is "<prefix>.aaa.bbb.ccc@<receiver host>" where
     *      aaa is the customer ID, and
     *      bbb is the instance ID, and
     *      ccc is the eventQueue ID
     *
     *	Examples: "kc.aaa.bbb.ccc@host.com"
     *		      "nc.125985.356.0@edm.netcentives.com"
     *
     */
    public boolean parseContextFromString(String from)
    {
        if (from == null) {
        	throw new IllegalArgumentException("null address");
        }

        // REVIEW: we won't need to call extractAddress when the address
        //		   come from the "envelope"!
        from = extractAddress(from);

        int atIndex = from.indexOf('@');
        if (atIndex == -1) {
        	//logger.error("Unexpected reply message--error in parsing context: from=" + from);
            return false;
        }
        
        senderDomain = from.substring(atIndex + 1);		// everything after '@'
        String _addr = from.substring(0, atIndex);		// everything before '@' 
        String[] vals = _addr.split("\\.");				// String.split takes regex so quote '.'
        
        // REVIEW1: do not use Numbers here, add catch(ParseException) and generate error.
        //if (vals.length == 3 ) {
        //	setCustomerId(Numbers.getLong(vals[0], DataRow.INVALID));
        //    setInstanceId(Numbers.getLong(vals[1], DataRow.INVALID));
        //    setEventQueueId(Numbers.getLong(vals[2], DataRow.INVALID));
        //} else if (vals.length == 4) {
        //	setCustomerId(Numbers.getLong(vals[1], DataRow.INVALID));
        //    setInstanceId(Numbers.getLong(vals[2], DataRow.INVALID));
        //   setEventQueueId(Numbers.getLong(vals[3], DataRow.INVALID));

		// Fix for Bug 70576
		// Modified by Raja.S (HCL Technologies)
		// Code changed to parse the email ID, correctly even if the 
		// prefix String contains any number of period characters
		if (vals.length >=3) {
			setCustomerId(getLong(vals[(vals.length - 3)], 0));
			setInstanceId(getLong(vals[(vals.length - 2)], 0));
			setEventQueueId(getLong(vals[(vals.length - 1)], 0));
			setSenderPrefix(vals);
			return true;
		} else {
			//logger.error("Unexpected reply message--error in parsing context: from=" + from);
			return false;
		}
    }

    /**
     * Extracts the email address from a To: or From: field.
     *
     * Example: John Smith <jsmith@host.com> would return jsmith@host.com
     * Example: "John Smith" <jsmith@host.com> would return jsmith@host.com
	 * FUTURE: Does not work for <address> "name".
     *
     * @param   addr    an email address
     * @return          the basic email address striped of extra names, etc.
     * @since   CRM1.0
     */
    private String extractAddress(String addr)
    {
        int startIndex, endIndex;

        startIndex = addr.indexOf('\"');
        if (startIndex != -1) {
            startIndex++;
            endIndex = addr.indexOf('\"', startIndex);
            if (endIndex != -1) {
                addr = addr.substring(endIndex + 1).trim();
            }
        }

        startIndex = addr.indexOf('<');
        String _res;
        if (startIndex != -1) {
            startIndex++;
            endIndex = addr.indexOf('>', startIndex);
            if (endIndex == -1) {
            	_res = addr.substring(startIndex);
            }
            else {
            	_res = addr.substring(startIndex, endIndex);
            }
            return _res;
        } else {
        	_res = addr.trim();
        	return _res;
        }
    }
    
    // untokenize the parsedAddr to get the sender prefix
    private void setSenderPrefix(String[] parsedAddr)
    {
    	int palen = parsedAddr.length;
    	if (palen < 3) {
    		throw new IllegalArgumentException("invalid message context: addr component count: " + palen);
    	}
    	switch (palen - 3) {
    	case 0:
    		senderPrefix = null;
    		break;
    	case 1:
    		senderPrefix = parsedAddr[0];
    		break;
    	default:
    		StringBuffer buf = new StringBuffer();
    		for (int i = 0; i < palen - 3; i++) {
    			if (i > 0) {
    				buf.append('.');
    			}
    			buf.append(parsedAddr[i]);
    		}
    		senderPrefix = buf.toString();
    	}
    }
    
    /**
     * Returns the long value of the specified string. Or, if any error
     * occurs, the default value is returned.
     *
     * @param   spec    the string to be parsed.
     * @param   def     the default value to be returned in case of error.
     * @return          the parsed value of <code>spec</code>, or <code>def</code> on error.
     */
    public static long getLong(String spec, long def)
    {
        try {
            return Long.parseLong(spec);
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public String printToString()
    {
    	StringBuffer buf = new StringBuffer();
    	buf.append(customer_id);
    	buf.append(".");
    	buf.append(instance_id);
    	buf.append(".");
    	buf.append(event_queue_id);
    	return buf.toString();	    	
    }

    public String toString()
    {
    	StringBuffer buf = new StringBuffer();
    	buf.append("[message-context: ");
    	buf.append(customer_id);
    	buf.append(".");
    	buf.append(instance_id);
    	buf.append(".");
    	buf.append(event_queue_id);
    	buf.append(" ]");
    	return buf.toString();	
    }
}
