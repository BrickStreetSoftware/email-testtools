package brickst.emailtest;

import java.util.Properties;

/**
 * Simplest Sender that returns the test data
 * @author cmaeda
 *
 */
public class InMemorySender implements Sender
{
	
	@Override
	public void initFromProperties(Properties props)
	{
	}

	@Override
	public Object sendTestMessage(TestCase test) throws Exception 
	{
		String msg = test.generateMessage();
		return msg;
	}
	
}
