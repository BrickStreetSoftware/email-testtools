package brickst.emailtest;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver program for email tester
 * 
 * The test framework has 
 */
public class Main
{
	private static Logger logger = LoggerFactory.getLogger(Main.class);

	String configFile;
	Properties configProps;

	TestCaseGroup testCaseGroup;
	Sender testSender;
	
	File inputFile;
	
    // load properties from file
    public static Properties loadProperties(String filename) throws IOException
    {
    	Properties props = new Properties();
    	FileReader reader = null;
            
    	try {
    		reader = new FileReader(filename);
    		props.load(reader);
    		return props;
    	}
    	finally {
    		if (reader != null) {
    			try { reader.close(); } catch (Throwable th) {}
    		}
    	}
    }
	
    public List<Object> runTests() throws Exception
    {    	
    	List<Object> results = new ArrayList<Object>();
    	for (int i = 0; i < testCaseGroup.size(); i++)
    	{
    		TestCase tc = testCaseGroup.get(i);
    		try
    		{
    			Object result = testSender.sendTestMessage(tc);
    			results.add(result);
    		}
    		catch (Throwable th)
    		{
    			results.add(th);
    		}
    	}
    	return results;
    }
    
    public void readInputFile(File input) throws IOException
    {
    	testCaseGroup.readInputFile(input);
    }
    
    public void initFromProperties(Properties p) throws Exception
    {
    	configProps = p;

		String testGroupClassName = configProps.getProperty("testGenClass");
		testCaseGroup = (TestCaseGroup) Class.forName(testGroupClassName).newInstance();
    	testCaseGroup.initFromProperties(configProps);
		
		String senderClassName = configProps.getProperty("testSenderClass");
		testSender = (Sender) Class.forName(senderClassName).newInstance();
    	testSender.initFromProperties(configProps);
    }
    
	public static Main parseArgs(String[] args) throws Exception
	{		
		// read config file
		String configFile = args[0];
		Properties configProps = null;
		try {
			configProps = loadProperties(configFile);
		}
		catch (Throwable th) 
		{
			logger.error("Load config {}", configFile, th);
			return null;
		}	
		
		Main m = new Main();
		m.configFile = configFile;
		m.initFromProperties(configProps);
		
		// read csv test properties
		String inputName = args[1];
		File inputFile = new File(inputName);
		if (! inputFile.exists())
		{
			logger.error("Invalid input file {}", inputFile.getAbsolutePath());
			return null;
		}
		m.readInputFile(inputFile);

		return m;
	}
	
	public static void usage()
	{
		logger.error("Usage: config-file input-file");
	}

	public static void main(String[] args)
	{
		//
		// PARSE ARGUMENTS
		//

		Main main = null;
		try
		{
			main = parseArgs(args);
		}
		catch (Throwable th)
		{
			logger.error("Parse Args", th);
		}
		if (main == null)
		{
			usage();
			System.exit(1);
		}

		//
		// RUN TESTS
		//
		
		List<Object> results = null;
		
		try
		{
			results = main.runTests();			
		}
		catch (Throwable th)
		{
			logger.error("Running Tests", th);
			System.exit(1);
		}
		
		logger.info("Test Results:");
		for (int i = 0; i < results.size(); i++)
		{
			Object res = results.get(i);
			if (res instanceof Throwable)
			{
				Throwable th = (Throwable) res;
				logger.info("{} Trial (ERROR)", i, th);
			}
			else
			{
				logger.info("{} Trial {}", i, res);
			}
		}
			
		// needed to terminate any runtimes
		System.exit(0);
	}
    
}


  
