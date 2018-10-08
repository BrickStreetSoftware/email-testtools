package brickst.emailtest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class TestVelocityTestGroup
{
	@Test
	public void readCsvTestGroupA() throws Exception
	{
		Properties props = new Properties();
		props.setProperty("csv.readColumnHeaders", "true");
		props.setProperty("csv.rowFilter", "A");
		props.setProperty("csv.templateDir", "testData/templates");
		
		File csv = new File("testData/csvdata/test-cases-1.csv");
		VelocityTestCaseGroup grp = new VelocityTestCaseGroup(props);
		grp.readInputFile(csv);
		
		Assert.assertTrue("Test Count", grp.size() == 3);
		VelocityTestCase vtc = (VelocityTestCase) grp.get(0);
		Assert.assertEquals("TestSeries", "A", vtc.testSeries);
		Assert.assertEquals("TestCase", "A1", vtc.testCase);
		Assert.assertEquals("Template", "kcxml1.vm", vtc.templateName);

		vtc = (VelocityTestCase) grp.get(1);
		Assert.assertEquals("TestSeries", "A", vtc.testSeries);
		Assert.assertEquals("TestCase", "A2", vtc.testCase);
		Assert.assertEquals("Template", "kcxml1.vm", vtc.templateName);

		vtc = (VelocityTestCase) grp.get(2);
		Assert.assertEquals("TestSeries", "A", vtc.testSeries);
		Assert.assertEquals("TestCase", "A3", vtc.testCase);
		Assert.assertEquals("Template", "kcxml1.vm", vtc.templateName);
	}
	
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
	public void testSimpleTestGroupA() throws Exception
	{
		Properties props = new Properties();
		props.setProperty("csv.readColumnHeaders", "true");
		props.setProperty("csv.rowFilter", "A");
		props.setProperty("csv.templateDir", "testData/templates");
		
		File csv = new File("testData/csvdata/simple1.csv");
		VelocityTestCaseGroup grp = new VelocityTestCaseGroup(props);
		grp.readInputFile(csv);
		
		Assert.assertTrue("Test Count", grp.size() == 3);
		VelocityTestCase vtc = (VelocityTestCase) grp.get(0);
		Assert.assertEquals("TestSeries", "A", vtc.testSeries);
		Assert.assertEquals("TestCase", "A1", vtc.testCase);
		Assert.assertEquals("Template", "simple1.vm", vtc.templateName);
		
		String msg = vtc.generateMessage();
		Map<String,String> varMap = parseSimpleOutput(msg);
		Assert.assertTrue("Var Count", varMap.size() == 3);
		Assert.assertEquals("FNAME", "George", varMap.get("FNAME"));
		Assert.assertEquals("LNAME", "Washington", varMap.get("LNAME"));
		Assert.assertEquals("EMAIL", "gw@example.com", varMap.get("EMAIL"));

		vtc = (VelocityTestCase) grp.get(1);
		Assert.assertEquals("TestSeries", "A", vtc.testSeries);
		Assert.assertEquals("TestCase", "A2", vtc.testCase);
		Assert.assertEquals("Template", "simple1.vm", vtc.templateName);

		msg = vtc.generateMessage();
		varMap = parseSimpleOutput(msg);
		Assert.assertTrue("Var Count", varMap.size() == 3);
		Assert.assertEquals("FNAME", "Sam", varMap.get("FNAME"));
		Assert.assertEquals("LNAME", "Adams", varMap.get("LNAME"));
		Assert.assertEquals("EMAIL", "sam@example.com", varMap.get("EMAIL"));

		vtc = (VelocityTestCase) grp.get(2);
		Assert.assertEquals("TestSeries", "A", vtc.testSeries);
		Assert.assertEquals("TestCase", "A3", vtc.testCase);
		Assert.assertEquals("Template", "simple1.vm", vtc.templateName);

		msg = vtc.generateMessage();
		varMap = parseSimpleOutput(msg);
		Assert.assertTrue("Var Count", varMap.size() == 3);
		Assert.assertEquals("FNAME", "Thos", varMap.get("FNAME"));
		Assert.assertEquals("LNAME", "Jefferson", varMap.get("LNAME"));
		Assert.assertEquals("EMAIL", "tj@example.com", varMap.get("EMAIL"));
	}
	
}