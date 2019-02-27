/**
 * @(#)IRandom.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

/**
 * Interface defines the method(s) needs to be implemented by tester.
 *
 * @author Jim Mei
 */
interface IRandom
{
    public void setRandom(double percent);
    public double getRandom();
}
