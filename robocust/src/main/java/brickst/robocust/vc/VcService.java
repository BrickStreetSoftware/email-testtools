/**
 * @(#)VcService.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.service.*;
import brickst.robocust.smtp.*;
import org.apache.log4j.Logger;

/**
 * VcService
 */
class VcService implements IRandom
{
	static Logger logger = Logger.getLogger(VcService.class);
    private Service service;
    private double random;
    
    VcService(Service service, double random) 
    {
        this.service = service;
        this.random = random;
    }
    
    Service getService() { return service; }
    public void setRandom(double random) { this.random = random; }
    public double getRandom() { return random; }
    
    boolean isApplicable() 
    {
        double rand = Math.random();
        if (rand < random)
            return true;
        else 
            return false;
    }

    void request(SmtpMessage msg) 
    {
        if (!isQueueFull(this.service)) {
            service.request(new ReceivedMailTestServiceRequest(msg));
        }
        else {
        	logger.info(service.toString() + ": queue full");
        }
    }
    
    public String toString() 
    {
        return service + ", random: " + random;
    }

    /**
    * This method returns true if the given service's threads are 
    * all occupied with work, and false if not.  Knowing this bit 
    * of information lets us queue up the most recent instance
    * for work or prep.
    * @param s the service who's queue you want to check
    * @return true if all threads are occupied and false if not.
    */
    private boolean isQueueFull(Service s)
    {
        ServiceProvider			provider = s.getServiceProvider();
        ServiceQueueManager		sqm = provider.getServiceQueueManager();
        SingleServiceQueueManager	ssqm = (SingleServiceQueueManager) sqm;

        return(ssqm.isQueueFull()); 
    }
        
}
