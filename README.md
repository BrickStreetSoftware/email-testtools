# email-testtools

This program provides an extensible framework for testing messaging systems. The framework is made up of 3 elements:

1. A test case generator that generates messages based on templates and CSV data files.
  * The TestCase and TestCaseGroup interfaces
2. A message Sender, responsible for delivering messages to the test system
  * Sender interface
3. A Result Checker, responsible for verifying the results of sending the messages to the system under test (SUT)
  * ResultChecker interface
  
The program is invoked with a configuration properties file and an input file.

Main config.properties testparams.csv

The configuration file controls all aspects of the test run.

