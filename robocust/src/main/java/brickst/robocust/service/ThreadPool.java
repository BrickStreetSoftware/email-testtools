/**
 * @(#)SingleServiceQueueManager.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 * Pool of threads at a specific priority
 */
public class ThreadPool
{
	static Logger logger = Logger.getLogger(ThreadPool.class);

    /** the priority to set for all threads we create. */
    private int threadPriority; // = Thread.NORM_PRIORITY;

    /** how many threads we will run concurrently. */
    private int maxThreads;

    /** Keep at least this many waiting (idle) threads. */
    private static final int minThreadsIdle = 10;

    /** Keep at most this many waiting (idle) threads. */
    private static final int maxThreadsIdle = 20;

    /** current count of threads idling */
    private int threadsWaiting;

    /** total number of threads alive for this pool */
    private int totalThreadCount;

    /** number of work items to be processed */
    private int csrCurrent;

    private ThreadCountManager daemon = new ThreadCountManager();

    public ThreadPool(int priority, int maxThreadCount)
    {
	    this.threadPriority = priority;
	    this.maxThreads = maxThreadCount;
	    threadPoolList.add(this);
	    daemon.start();
    }

    public String toString()
    {
	return	"ThrPool: pri=" + threadPriority +
		" maxThreads=" + maxThreads +
		" waiting=" + threadsWaiting +
		" removing=" + threadsToRemove +
		" total=" + totalThreadCount +
		" csrCurrent=" + csrCurrent +
		" adjustments=" + countThreadAdjustments;
    }

    public static ArrayList<ThreadPool> threadPoolList = new ArrayList<ThreadPool>();

    public static ThreadPool getThreadPool(Thread thread)
    {
    	return getThreadPool(thread.getPriority());
    }

    public static ThreadPool getThreadPool(int priority)
    {
		if (priority != Thread.MAX_PRIORITY &&
	                priority != Thread.NORM_PRIORITY &&
	                priority != Thread.MIN_PRIORITY)
		{
			logger.error("unknown thread priority = " + priority);
		}
		
		for (int i=0; i<threadPoolList.size(); i++) {
		    ThreadPool tp = threadPoolList.get(i);
		    if (tp.getThreadPriority() == priority) {
			    return tp;
		    }
		}
		return null;
    }

    public int getThreadPriority() { return(threadPriority); }
    public void addThreads(int count) { maxThreads += count; }
    public void removeThreads(int count) { maxThreads -= count; }

    /**
    *  Called by client to notify this pool a
    *  that a new work item has been added
    */
    public synchronized void notifyEnqueue(int csrCurrent)
    {
	this.csrCurrent = csrCurrent;
	notify();
    }

    /**
    * Called by threads to wait
    */
    public synchronized boolean threadWait()
    {
	threadsWaiting++;
	waitOnThis(); // wait on CRMObject
	threadsWaiting--;

	return(shouldThreadTerminate());
    }

    /** Called by daemon.  Must be private and synchronized. */
    private synchronized String adjustThreadCount()
    {
        String operation;		// DEBUG
        if (totalThreadCount >= (maxThreads + minThreadsIdle)) {
            return("Too many threads!");
        } else if (threadsWaiting < minThreadsIdle)  {
            if (csrCurrent > 100) {
                for (int i=0; i<10; i++) {
                    ServiceThread thread = new ServiceThread();
                    thread.setPriority(threadPriority);
                    thread.start();
                    totalThreadCount++;
                    updateCountThreadAdjustments("create");
                }
                return("added 10 threads");
            }
            // Create service thread.
            ServiceThread thread = new ServiceThread();
            thread.setPriority(threadPriority);
            thread.start();
            totalThreadCount++;
            updateCountThreadAdjustments("create");
            return("Added thread.");
        } else if ((threadsWaiting - threadsToRemove) > maxThreadsIdle)  {
            // Terminate service thread.
            threadsToRemove++;
            notify();
            updateCountThreadAdjustments("remove");
            return("Removing thread.");
        }

        return(null);
    }
    private void updateCountThreadAdjustments(String operation)
    {
        ++countThreadAdjustments;
        logger.debug("Thread " + operation + " " + this);
    }
    private int countThreadAdjustments = 0;

    private int threadsToRemove = 0;

    // NB: Must be called from synchronized method.
    private boolean shouldThreadTerminate()
    {
        if (threadsToRemove > 0)  {
            threadsToRemove--;
            totalThreadCount--;
            return(true);
        }

        return(false);
    }

    /** Wait this long between adjusting thread count. */
    private static final int msSleepBetweenWaiterCountCheck = 5000;	// 5 seconds

    /** We don't want to use the timer, since that relies on having service
     * threads. */
    private class ThreadCountManager extends Thread
    {
        ThreadCountManager() { }

        public void run()
        {
            while (true)  {
                String result = adjustThreadCount();
                try {
                	Thread.sleep(msSleepBetweenWaiterCountCheck);
                }
                catch (InterruptedException ix) {
        	        Thread.currentThread().dumpStack();
                }
            }
        }
    }
    public final synchronized void waitOnThis()
    {
        try {
	        wait();
        } catch (InterruptedException e) {
	        Thread.currentThread().dumpStack();
        }
    }

}