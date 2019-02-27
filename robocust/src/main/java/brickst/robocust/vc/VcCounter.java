/**
 * @(#)VcCounter.java
 *
 * Copyright (c) 2001 by Kana Software. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.CRMTime;

/**
 * VcCounter will be used to count a feature/action's throughput and latency.
 * 
 * @author CJ Lofstedt 8/20/01
 *         Jim Mei     09/20/01
 */
public class VcCounter
{
    private String name; //counter's name
    //real time values
    private int value = 0;
    private long maxLatency = 0;
    private long totalLatencyInThisPeriod = 0;
    private long maxLatencyThisPeriod = 0;
    
    //aggregrate values of last period
    private double throughput = 0;
    private int throughputValue = 0;
    private long averageLatency = 0;
    private long maxLatencyLastPeriod = 0;
 
    private int lastReportValue = 0;
    private long lastReportTime = 0;

    VcCounter() 
    { 
        this(""); 
    }
    
    VcCounter(String name) 
    { 
        this.name = name; 
        lastReportTime = System.currentTimeMillis();
    }
    
    public synchronized void increment()
    {
        value++;
    }

    public synchronized void increment(long latency)
    {
        increment();
        addLatency(latency);
    }

    private synchronized void addLatency(long latency)
    {
        totalLatencyInThisPeriod += latency;
        if (latency > maxLatency) maxLatency = latency;
        if (latency > maxLatencyThisPeriod) maxLatencyThisPeriod = latency;
    }
    
    public int getValue() { return value; }
    
    public String getName() { return this.name; }
    
    public String getReport() 
    { 
        StringBuffer report = new StringBuffer(CRMTime.getDateString() + " " + name + ": ");
        report.append(getReportShort());
        return report.toString();
    }
    
    public synchronized String doReport()
    {
        long reportTime = System.currentTimeMillis();
        if (reportTime == lastReportTime) 
            return getReportShort();
        throughputValue = value - lastReportValue;
        throughput = 1000.0 * throughputValue / (reportTime - lastReportTime);
        averageLatency = 
                (throughputValue == 0)? 0 : totalLatencyInThisPeriod / throughputValue;
        maxLatencyLastPeriod  = maxLatencyThisPeriod;
        
        resetPeriodData();
        
        return getReportShort();
    }
    
    private String getReportShort()
    {
        return throughput + ", " + throughputValue + ", " + 
                averageLatency + ", " + maxLatencyLastPeriod + ", " + value;
    }
    
    private void resetPeriodData()
    {
        lastReportValue = value;
        lastReportTime = System.currentTimeMillis();
        totalLatencyInThisPeriod = 0;
        maxLatencyThisPeriod = 0;
    }
    
}
