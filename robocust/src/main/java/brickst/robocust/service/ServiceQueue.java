/**
 * @(#)ServiceQueue.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import java.util.LinkedList;
import org.apache.log4j.Logger;

/**
 * The ServiceQueue class tracks a set of ServiceQueueElement's, providing
 * synchronized enqueue and dequeue operations.  It throttles callers to enqueue
 * when the queue is full; it returns failure from dequeue when queue is empty.
 * This asymmetry supports Service Thread migration (see ServiceList class).
 * <br>
 * ServiceQueue used to extend ListItem so it could be a member of a
 * ServiceQueueList.  However, no one uses ServiceQueueList anymore, so we have
 * removed the ListItem superclass
 */
public class ServiceQueue
{
	static Logger logger = Logger.getLogger(ServiceQueue.class);
    private static final int csqeMaxDefault = 10;	// FUTURE: Get from Config.

    /** the maximum number of ServiceQueueElements to allow in the queue.
    * Writers block when the queue is full. */
    int csqeMax;

    /** Return queue length for this service queue */
    public int getQueueLength() { return csqeMax; }
    
    /** Reconfigure the max queue length */
    public synchronized void setQueueLength(int maxlen)
    {
        csqeMax = maxlen;
    }
    
    // FUTURE: I've removed service from here.  We don't seem to care and it
    // is inconvenient to some callers who don't know.
    // Service service;		// Parent service for this SQ.  (Do we care?)

    /** the list of ServiceQueueElement's, accessed as FIFO. */
    LinkedList<ServiceQueueElement> listSQE = new LinkedList<ServiceQueueElement>();  // FUTURE: Instantiate Trace UI peer here.

    /**
    * Constructs a new ServiceQueue.
    *
    * @param csqeMax	the maximum number of ServiceQueueElements to allow
    *					in the queue).
    */
    public ServiceQueue()
    {
        this(csqeMaxDefault);
        //{{INIT_CONTROLS
        //}}
    }
    public ServiceQueue(int csqeMax)
    {
        this.csqeMax = csqeMax;
    }

    /**
    * Returns true iff queue is empty.
    * @return true iff queue is empty.
    */
    public boolean isQueueEmpty() { return(listSQE.isEmpty()); }

    /**
    * Returns true iff queue is full.
    * @return true iff queue is full.
    */
    public boolean isQueueFull() { 
		if (logger.isDebugEnabled()) {
	        logger.debug("itemcount: " + listSQE.size() + ", max: " + csqeMax);
		}	
        return(listSQE.size() >= csqeMax); }

    /**
    * Tries to enqueue the ServiceQueueElement.  Returns true iff element
    * enqueued; false if queue was full.
    *
    * @param sqe	the ServiceQueueElement to enqueue.
    * @return	true iff sqe was enqueued successfully; false if queue was full.
    */
    public synchronized boolean tryEnqueueSQE(ServiceQueueElement sqe)
    {
        if (isQueueFull())
	        return(false);

        enqueueSQE(sqe);
        return(true);
    }

    /**
    * Enqueues a ServiceQueueElement onto the service queue.
    *
    * @param sqe	the ServiceQueueElement to enqueue.
    */
    public synchronized void enqueueSQE(ServiceQueueElement sqe)
    {
        // If the queue is full, wait for signal from readers.
        while (isQueueFull())  {
            waitOnThis();
        }

        listSQE.addLast(sqe);
    }

    /**
    * Tries to dequeue a ServiceQueueElement from the queue.
    *
    * @return	the ServiceQueueElement removed from the queue; false if the
    *			queue was empty.
    */
    public synchronized ServiceQueueElement tryDequeueSQE()
    {
        return(isQueueEmpty() ? null : dequeueSQEKnown());
    }

    /**
    * Dequeues a ServiceQueueElement from the queue.  Queue must not be empty.
    *
    * @return	the ServiceQueueElement removed from the queue.
    */
    private synchronized ServiceQueueElement dequeueSQEKnown()
    {
        // REVIEW: I've changed the notify logic here to try to fix the
        // problems enqueue hanging on full non-full service queues.
	    //		boolean	fNotify = isQueueFull();

        ServiceQueueElement	sqe = listSQE.removeFirst();		

	    //		if (fNotify)  {
	    // FUTURE/REVIEW/WARNING: If readers ever wait on the ServiceQueue,
		// use notifyAll() instead of notify().  Although, it seems that
	    // if queue is full then there should be no readers.
	    	 notifyAll();
	    //		}

        return(sqe);
    }

    /** Removes all elements from the queue. */
    public synchronized void removeAll()
    {
        while (!isQueueEmpty())
            dequeueSQEKnown();

        notifyAll();	// Wake writers.
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
