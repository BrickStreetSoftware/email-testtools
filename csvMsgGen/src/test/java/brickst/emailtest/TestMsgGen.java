package brickst.emailtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class TestMsgGen
{
	private Map<String,String> parseSimpleOutput(String s) throws IOException
	{
		Map<String,String> varMap = new HashMap<String,String>();
		
		StringReader sr = new StringReader(s);
		BufferedReader br = new BufferedReader(sr);
		while (true)
		{
			String line = br.readLine();
			if (line == null)
			{
				break;
			}
			String[] kv = line.split("=");
			if (kv.length == 2)
			{
				String k = kv[0];
				String v = kv[1];
				varMap.put(k, v);
			}
		}
		return varMap;
	}

	
	@Test
	public void simpleTest1() throws Exception
	{
		Properties props = new Properties();
		props.setProperty("csv.readColumnHeaders", "true");
		props.setProperty("csv.rowFilter", "A");
		props.setProperty("csv.templateDir", "testData/templates");
		props.setProperty("testGenClass", "brickst.emailtest.VelocityTestCaseGroup");
		props.setProperty("testSenderClass", "brickst.emailtest.InMemorySender");
		
		File csv = new File("testData/csvdata/simple1.csv");
		
		Main m = new Main();
		m.initFromProperties(props);
		m.readInputFile(csv);
		
		List<Object> results = m.runTests();
		Assert.assertTrue("Result Count", results.size() == 3);
		
		String msg = (String) results.get(0);
		Map<String,String> varMap = parseSimpleOutput(msg);
		Assert.assertTrue("Var Count", varMap.size() == 3);
		Assert.assertEquals("FNAME", "George", varMap.get("FNAME"));
		Assert.assertEquals("LNAME", "Washington", varMap.get("LNAME"));
		Assert.assertEquals("EMAIL", "gw@example.com", varMap.get("EMAIL"));
		
		msg = (String) results.get(1);	
		varMap = parseSimpleOutput(msg);
		Assert.assertTrue("Var Count", varMap.size() == 3);
		Assert.assertEquals("FNAME", "Sam", varMap.get("FNAME"));
		Assert.assertEquals("LNAME", "Adams", varMap.get("LNAME"));
		Assert.assertEquals("EMAIL", "sam@example.com", varMap.get("EMAIL"));

		msg = (String) results.get(2);	
		varMap = parseSimpleOutput(msg);
		Assert.assertTrue("Var Count", varMap.size() == 3);
		Assert.assertEquals("FNAME", "Thos", varMap.get("FNAME"));
		Assert.assertEquals("LNAME", "Jefferson", varMap.get("LNAME"));
		Assert.assertEquals("EMAIL", "tj@example.com", varMap.get("EMAIL"));
	
	}
}
