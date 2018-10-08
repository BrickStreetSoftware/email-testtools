package brickst.emailtest;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public interface TestCaseGroup 
{
	public void initFromProperties(Properties props) throws Exception;
	
	public void readInputFile(File file) throws IOException;
	
	/**
	 * Returns number of test cases in group
	 * @return
	 */
	public int size();
	
	public TestCase get(int i);
	
	public TestCase getRandom();
}
