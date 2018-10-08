package brickst.emailtest;

import java.util.Properties;

/**
 * Represents a component that delivers a test message
 * @author cmaeda
 *
 */
public interface Sender 
{
	public void initFromProperties(Properties props);

	public Object sendTestMessage(TestCase test) throws Exception;
}
