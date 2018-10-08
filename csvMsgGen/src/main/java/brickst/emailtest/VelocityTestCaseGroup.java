package brickst.emailtest;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.velocity.app.VelocityEngine;

import com.opencsv.CSVReader;

public class VelocityTestCaseGroup implements TestCaseGroup
{
	Properties config;
	File csvFile;

	String rowFilter;
	boolean readColHeaders;
	String templateBase;
	
	// raw data from file
	List<String[]> rows;
	String[] headerRow;

	Map<String,Integer> nameToColumnIndexMap;
	List<VelocityTestCase> testCases;
	
	// name to column index map
	Map<String,Integer> nameToColMap;
	
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
	 * Scans a string array; returns true if all elements are null or empty
	 * @param line
	 * @return
	 */
	private static boolean isBlankLine(final String[] line)
	{
		// empty line?
		if (line == null || line.length == 0)
		{
			return true;
		}
		
		// line has elements, check each one
		for (int i = 0; i < line.length; i++) 
		{
			String elt = cleanString(line[i]);
			if (elt != null) 
			{
				// found an element that is not blank
				return false;
			}
		}
		return true;
	}

	/**
	 * Builds a name-to-column index map
	 * @param headerRow
	 * @return
	 */
	private static Map<String,Integer> buildNameToColMap(String[] headerRow)
	{
		Map<String,Integer> colMap = new LinkedHashMap<String,Integer>();
		
		for (int i = 0; i < headerRow.length; i++)
		{
			String colName = cleanString(headerRow[i]);
			colMap.put(colName, i);			
		}
		
		return colMap;		
	}
	
	public VelocityTestCaseGroup()
	{		
	}
	
	public VelocityTestCaseGroup(Properties props) throws Exception
	{		
		initFromProperties(props);
	}
	
	@Override
	public void initFromProperties(Properties props) throws Exception
	{
		config = props;

		rowFilter = cleanString(config.getProperty("csv.rowFilter"));
		templateBase = cleanString(config.getProperty("csv.templateDir"));
		
		String pval = cleanString(config.getProperty("csv.readColumnHeaders"));
		if ("true".equalsIgnoreCase(pval) || "yes".equalsIgnoreCase(pval))
		{
			readColHeaders = true;
		}
		else
		{
			readColHeaders = false;
		}
	}

	/**
	 * Read test case params from a CSV file
	 * @param csv
	 * @throws IOException 
	 */
	public void readInputFile(File csv) throws IOException
	{		
		csvFile = csv;
		FileReader fr = null;
		CSVReader csvReader = null;
		
		// init data vars		
		rows = new ArrayList<String[]>();
		headerRow = null;
		nameToColMap = null;
		testCases = null;
		
		// read raw data...
		try
		{
		    fr = new FileReader(csvFile);
		    csvReader = new CSVReader(fr);

		    while (true)
		    {
			    String [] nextLine = csvReader.readNext();
			    if (nextLine == null)
			    {
			    	// end of file
			    	break;
			    }		
			    
			    // skip blank lines
			    if (isBlankLine(nextLine))
			    {
			    	continue;	
			    }
			    
			    // keep first row (i.e. header row)
			    if (readColHeaders && headerRow == null)
			    {
			    	headerRow = nextLine;
			    	continue;
			    }
			    
			    // filter first row?
			    if (rowFilter != null)
			    {
			    	String firstCol = cleanString(nextLine[0]);
			    	if (! rowFilter.equalsIgnoreCase(firstCol))
			    	{
			    		// skip line that does not match firstColMatch
			    		continue;
			    	}
			    }
			    
			    // row matches; add to result
			    rows.add(nextLine);
		    }			
		}
		finally 
		{
			if (csvReader != null) 
			{
				try { csvReader.close(); } catch (Throwable th) { /* don't care */ }				
			}
			if (fr != null) 
			{
				try { fr.close(); } catch (Throwable th) { /* don't care */ }				
			}
		}		
		
		// prep TestCases
		if (headerRow != null)
		{
			Map<String,Integer> _nameToColMap = buildNameToColMap(headerRow);
			nameToColMap = _nameToColMap; 
		}
		
		// set up velocity
		VelocityEngine engine = new VelocityEngine();
		Properties vProps = new Properties();
		if (templateBase != null)
		{
			vProps.setProperty("resource.loader", "file");
			vProps.setProperty("file.resource.loader.class", "org.apache.velocity.runtime.resource.loader.FileResourceLoader");
			vProps.setProperty("file.resource.loader.path", templateBase);			
		}
		engine.init(vProps);
		
		// init test cases
		testCases = new ArrayList<VelocityTestCase>();
		for (int i = 0; i < rows.size(); i++)
		{
			String[] row = rows.get(i);
			VelocityTestCase vtc = new VelocityTestCase(nameToColMap, row, engine);
			testCases.add(vtc);
		}		
	}
	
	
	
	@Override
	public int size()
	{
		return (testCases != null ? testCases.size() : 0);
	}

	@Override
	public TestCase get(int i) 
	{
		return testCases.get(i);
	}

	@Override
	public TestCase getRandom() 
	{
		int idx = ThreadLocalRandom.current().nextInt(testCases.size());
		return testCases.get(idx);
	}

}
