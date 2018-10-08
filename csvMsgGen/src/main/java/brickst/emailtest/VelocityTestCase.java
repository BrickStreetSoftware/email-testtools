package brickst.emailtest;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * Represents a test case implemented by a Velocity Template
 * @author cmaeda
 *
 */
public class VelocityTestCase implements TestCase 
{
	public String testSeries;
	public String testCase;
	public int dependencyOrder;	
	public String jmsTopic;
	public String templateName;
	public Template velocityTemplate;	
	public Map<String,String> testParams;
	
	public VelocityTestCase()
	{
		testParams = new HashMap<String,String>();
	}

	public VelocityTestCase(Map<String,Integer> columnMap, String[] params, VelocityEngine engine)
	{
		testParams = new HashMap<String,String>();
		initTestCase(columnMap, params, engine);
	}
	
	private static String cleanString(String s)
	{
		if (s == null)
		{
			return null;
		}
		s = s.trim();
		if (s.isEmpty())
		{
			return null;
		}
		return s;
	}

	/**
	 * Creates a test case from a row in a CSV file.
	 * Expect to find hardcoded column names.
	 * The columnMap object allows the columns to appear in any order.
	 * @param columnMap
	 * @param params
	 */
	public void initTestCase(Map<String,Integer> columnMap, String[] params, VelocityEngine engine)
	{
		Integer index;
		
		// test series
		index = columnMap.get("TestSeries");
		testSeries = cleanString(params[index]);
		// test case
		index = columnMap.get("TestCase");
		testCase = cleanString(params[index]);		
		// dependencyOrder
		index = columnMap.get("Order");
		dependencyOrder = Integer.parseInt(cleanString(params[index]));
		// templateName
		index = columnMap.get("Template");
		templateName = cleanString(params[index]);
		
		if (templateName == null)
		{
			throw new RuntimeException("No Template Name Found");
		}
		else
		{
			velocityTemplate = engine.getTemplate(templateName, "UTF-8");
		}
		
		// 
		// params for templates
		//
		if (testParams != null) 
		{
			testParams.clear();
		}
		else 
		{
			testParams = new HashMap<String,String>();			
		}
		
		for (String paramName : columnMap.keySet())
		{
			Integer colIndex = columnMap.get(paramName);
			String paramVal = cleanString(params[colIndex]);
			if (paramVal != null)
			{
				testParams.put(paramName, paramVal);
			}			
		}
	}
	
	@Override
	public String generateMessage()
	{
		VelocityContext context = new VelocityContext(testParams);
		
		StringWriter writer = new StringWriter();		
		velocityTemplate.merge(context, writer);
		String populated = writer.toString();
		return populated;
	}
}
