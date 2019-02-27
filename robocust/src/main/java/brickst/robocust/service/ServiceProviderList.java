/**
 * @(#)ServiceProviderList.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.service;

import java.util.LinkedList;
import org.apache.log4j.Logger;


/**
 * Concurrency: All non-private methods are synchronized.  See CRMServices for
 * locking hierarchy.
 */
public final class ServiceProviderList
{
	static Logger logger = Logger.getLogger(ServiceProviderList.class);

    /** the total number of service requests in all queues. */
    private int csrCurrent = 0;

    /** list of providers which have outstanding requests */
    private LinkedList<ServiceProvider> listProviders = new LinkedList<ServiceProvider>();

    /**
    * The next provider to start with in fetchNextRequest.
    */
    private int next;

    ServiceProviderList() { }

    private int cMaxThreadsProvidersNeed = 0;

    /**
    * Adds a service provider to a list of known service providers.
    * Service threads will now be able to handle requests from this provider.
    *
    * @param providerNew	the service provider to add.
    */
    synchronized void add(ServiceProvider provider)
    {
        int	priority = provider.getPriority();
        // adjust max thread count for the pool
        // whose priority is same as this provider's priority
        ThreadPool tp = ThreadPool.getThreadPool(priority);
        if (tp == null) {
			if (logger.isDebugEnabled()) logger.debug("create new pool for thread priority =" + priority);
            tp = new ThreadPool(priority, provider.getMaxServiceThreads());
			if (logger.isDebugEnabled()) logger.debug(tp.toString());
        } else {
			if (logger.isDebugEnabled()) logger.debug("add threads = " + provider.getMaxServiceThreads() + " to Pool" + tp);
            tp.addThreads(provider.getMaxServiceThreads());
        }

        listProviders.addLast(provider);
    }

    synchronized void remove(ServiceProvider provider)
    {
        //cMaxThreadsProvidersNeed -= provider.getMaxServiceThreads();
        ThreadPool tp = ThreadPool.getThreadPool(provider.getPriority());
        tp.removeThreads(provider.getMaxServiceThreads());

        listProviders.remove(provider);
    }

    /**
    * Fetches the next service request.  Called by getServiceThreadRequest to
    * search the provider list and locate a ServiceRequest to execute (or wait
    * until a thread is needed).
    * <br>
    * This is a separate method because we must hold the class lock to protect
    * static members csrCurrent and cReadersWaiting which is used by methods we
    * call.
    *
    * @param serviceThread			the calling thread, whose status we update.
    * @param serviceThreadRequest	the structure to fill in with attributes of
    *								the ServiceRequest assigned.
    *
    * @return	true if the ServiceThreadRequest object provided has been
    *			successfully populated; false if the calling thread should exit.
    */
    boolean fetchNextRequest(ServiceThread serviceThread,
			    ServiceThreadRequest serviceThreadRequest)
    {
        // Check whether thread should terminate.
        //if (shouldThreadTerminate())
        //	return(false);

        // serviceThread.updateStatusFast("Seek ServiceRequest");
        //Debug.assert(csrCurrent == 0  ||  cReadersWaiting == 0);

        while (true) {
            // Wait while no outstanding ServiceRequests.
            while (csrCurrent == 0)  {
                // Check whether thread should terminate.
                // Debug.SV.println("go to thread wait for " +
                //                    ThreadPool.getThreadPool(serviceThread));
                ThreadPool tp = ThreadPool.getThreadPool(serviceThread);

                if (tp.threadWait())
	                return(false);		// Terminate.

                // Debug.SV.println("wake up from wait for " +
                //                    ThreadPool.getThreadPool(serviceThread));
                if (csrCurrent == 0)
	                reportIdleWakeup();
            }

            synchronized (this) {
				int lpsize = listProviders.size();
                for(int i = lpsize; i > 0; i--)
                {
					if (next >= lpsize) {
						next = 0;
					}
	                ServiceProvider provider = listProviders.get(next);
	                if(provider == null) {
		                provider = listProviders.getFirst();
						next = 0;
					}

					next++;

	                if((provider.getPriority() >= serviceThread.getPriority()) &&
		                provider.tryDequeueServiceRequest(serviceThreadRequest))
	                {
		                csrCurrent--;	// Dequeue decrements obj count.
		                return(true);
	                }
                }
            }

            // We reach this point if csrCurrent is not 0 but the providers are
            // all busy.  The real goal is not to wake a thread unless work is
            // available for it; this would require dequeue at notifyEnqueue
            // into temporary ServiceThreadRequest, etc.
            reportUselessWakeup();

            // In any case, the best we can do is wait again.
            ThreadPool tp = ThreadPool.getThreadPool(serviceThread);
            if (tp.threadWait()) {
                // Debug.SV.println("wake from wait: terminating" );
                return(false);		// Terminate.
            } else {
                // Debug.SV.println("wake from wait: retry to dequeue" );
            }
        }
    }



    /**
    * Called by Service.enqueueRequest() with no locks held after enqueueing
    * a new ServiceRequest.  If the provider wants another thread, we use
    * this opportunity to wake up a reader if any is idling.
    * <p>
    * FUTURE: Much more smarts possible here: maybe hand off requests to
    * threads; decide to migrate threads if this Queue needs attention; etc.
    *
    * FUTURE: Hand off makes much sense:
    * <li> Provider shouldn't signal us unless it wants another thread.
    * <li> If we have idle threads, no other provider should need one a thread.
    * Therefore we should assign a thread to provider immediately.  Races
    * make it hard to do this.
    *
    * @param	provider			the service provider where the enqueue
    *								happened.
    * @param	request				the service request that was just enqueued.
    * @param	providerWantsThread	true if the provider wants another thread.
    */
    public synchronized void notifyEnqueue(
        ServiceProvider	provider,
        ServiceRequest	request,
        boolean			providerWantsThread)
    {
        //Debug.assert(csrCurrent == 0  ||  cReadersWaiting > 0);

        csrCurrent++;

        if (providerWantsThread) { //&&  cReadersWaiting > 0) {
            // Debug.SV.println("notify: " +
            //     ThreadPool.getThreadPool(provider.getPriority()));
            ThreadPool tp = ThreadPool.getThreadPool(provider.getPriority());
            tp.notifyEnqueue(csrCurrent);
        }
    }


    /** helpers for checkServiceProgress */
    private long msLastErrorEscalation = 0;
    private boolean shouldEscalate()
    {
        long msNow = System.currentTimeMillis();
        // We will escalate every getMsHangBeforeEscalatingError() (eg, 15 min.)
        if ((msNow - msLastErrorEscalation) > getMsHangBeforeEscalatingError()){
            msLastErrorEscalation = msNow;
            return(true);
        } else {
            return(false);
        }
    }

    private static long getMsHangBeforeReporting()
    { return(getMsHangBeforeEscalatingError() / 3); }

    private static long getMsHangBeforeEscalatingError()
    { return(1000 * 900); }


    //
    // DEBUGGING CODE TO EOF
    //

    /**
    * Called when a thread has been awakened, but csrCurrent == 0.
    *
    * FUTURE: This seems to happen when we are just barely keeping up with our
    * receivers, and thus barely keeping our service queues empty.  What
    * happens is that a thread which has finished (or is just finishing) its
    * work gets the request that triggered the wakeup.
    *
    * The best solution I can think of is to delay waking threads until there
    * are several outstanding requests.  Not too smart, but it may work well
    * enough.  Just don't delay if there aren't "enough" threads servicing
    * objects to avoid inter-component starvation/deadlock.
    */
    private synchronized void reportIdleWakeup()
    { 
    	idleWakeups++;
    	if (logger.isDebugEnabled()) {
    		logger.debug("Idle Thread Wakeups "+ idleWakeups);
    	}
    }
    private int idleWakeups = 0;

    /**
    * Called when a thread has been awakened, and csrCurrent > 0, but no
    * service provider can tolerate another thread.
    *
    * FUTURE: Clearly, we can fix this.  How?  I don't know.  It's complicated
    * because joining a service provider really needs to happen atomically with
    * deciding that a thread should be awakened.  But just because a thread has
    * awakened doesn't mean it has joined a provider.
    *
    * A fix might be this: in notifyEnqueue(), if there are readers waiting
    * then dequeue from the given SQ.  That would increment the join count on
    * success.  Then, append the ServiceRequest to a private list which threads
    * first check before walking the Services List.  The thread takes the SR
    * from the list, and the join count is already correct.  Effective,
    * perhaps; ugly, perhaps.
    */
    private synchronized void reportUselessWakeup()
    { 
    	uselessWakeups++;
    	if (logger.isDebugEnabled()) {
    		logger.debug("Useless Thread Wakeups " + uselessWakeups); 
    	}
    }
    private int uselessWakeups = 0;
}
