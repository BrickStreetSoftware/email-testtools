/**
 * @(#)VcReportProvider.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

/**
 */
public interface VcReportProvider
{
    /**
    * calculate status report and return to StringBuffer
    *
    * @param	buff    a StringBuffer to store the report
    */
    public abstract void getStatsReport(StringBuffer buff);
}
