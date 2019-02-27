package brickst.robocust.lib;

/**
 * The Counter class record changes to a counter It can also compute its peak
 * "rate" -- the ratio of the current count to the time passed.  This rate is
 * reset each 1000 objects so we can see peak  rates.
 * <br>
 * As an example, a class could use Counter to record the number of items it
 * has processed and the rate of that processing.
 * <br>
 * Currently the counter is stored as an integer and its rate is expressed in
 * number/seconds.
 * <br>
 * Concurrency:  all the public method are synchronized
 */
public class SimpleCounter // extends com.brickstreet.common.lib.CRMObject
	implements java.io.Serializable
{
	private static final long serialVersionUID = -5336179741333300546L;

	/** the current count. */
	private int count;

	/** the count at last resetRate() -- when msStarted was last reset to
	 * current. */
	private int countRateStart;

	/** the name prefix to use when we report a count. */
	private String reportingName = null;

	/** the interval with which we report count (when count % interval == 0). */
	private int countIntervalReport = 0;

	/**
	 * the time (in milliseconds) at which we started counting.  Initialized to
	 * time of construction.
	 *
	 * @see Counter#reset.
	 */
	private long msStarted;

	/** Constructs a counter. */
	public SimpleCounter()
	{
		reset();		// Initializes count and msStarted.
	}
 
	public synchronized void enableReporting(String reportingName, int countIntervalReport)
	{
		assert (countIntervalReport >= 0);	// 0 == no reporting
		this.reportingName = reportingName;
		this.countIntervalReport = countIntervalReport;
	}

	/**
	 * Adds a value to the counter.
	 *
	 * @param	addend	the value to add to the count.
	 * @return	the resulting count.
	 */
	public synchronized int add(int addend)
	{
		assert(count + addend >= 0);
		checkReport(addend);
		count += addend;
		return(count);
	}

	/**
	 * Adds one to the counter.
	 * @return the resulting count.
	 */
    public synchronized int increment()
	{
		checkReport(1);
		count++;
		return(count);
    }

	/** Reports to Log if adding addend to count will cross a
	 * countIntervalReport boundary. */
	public synchronized void checkReport(int addend)
	{
		if (reportingName == null  ||  countIntervalReport == 0)
			return;

		int magNew = (count + addend) / countIntervalReport;
		int magOld = count / countIntervalReport;
		for (int mag = magOld+1; mag <= magNew; mag++)  {
			/*
			Debug.SC.println("Counter: " + reportingName + ": " + 
						(mag * countIntervalReport) + " @ " +
						getRateString() + "/sec " + " @ " + new Date());
			*/
			resetRate();
		}
	}

	/**
	 * Subtracts one from the counter.
	 * @return the resulting count.
	 */
	public synchronized int decrement()
	{
		assert(reportingName == null);	// Ill advised if reporting!
		count--;
		return(count);
	}

	/**
	 * Returns the current value of the counter.
	 * @return the current value of the counter.
	 */
    public synchronized int getValue()
	{
		return(count);
    }

	/**
	 * Returns the current rate of the counter.
	 * @return the current rate of the counter.
	 */
    public synchronized float getRate()
	{
		long dms = System.currentTimeMillis() - msStarted;
		if (dms <= 200)		// Avoid strangely high rates when dms near 0.
			dms = 200;
		return(((float) (count - countRateStart)) * 1000 / dms);
    }

	/**
	 * Resets the counter to 0 and the start time to now.
	 */
    public synchronized void reset()
	{
		resetRate();
		count = 0;
    }

	/**
	 * Resets the counter to 0 and the start time to now.
	 */
    public synchronized void resetRate()
	{
		countRateStart = count;
		msStarted = System.currentTimeMillis();
    }

	/**
	 * Determines the rate.
	 *
	 * @return	the rate, as a string.
	 */
	public synchronized String getRateString()
	{
		float	rate = getRate();
		int		prefix = ((int) rate);
		int		suffix = ((int) (rate * 10)) % 10;

		assert(prefix >= 0  &&  suffix >= 0);

		// Don't show decimal precision if more than 3 figures.
		boolean showSuffix = (rate < 1000)  &&  (rate != 0);
		return("" + prefix + ( showSuffix ? ("." + suffix) : "" ));
	}
}
