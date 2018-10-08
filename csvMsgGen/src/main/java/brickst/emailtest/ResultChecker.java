package brickst.emailtest;

import java.util.List;
import java.util.Properties;

public interface ResultChecker 
{
	public void initFromProperties(Properties p) throws Exception;
	public void checkResults(List<Object> results) throws Exception;
}
