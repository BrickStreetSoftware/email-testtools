Virtual Customer 1.0

robocust@brickstreetsoftware.com
Mar 21, 2011

Virtual Customer (VCust) is a test program that receives incoming SMTP
messages.  For each incoming message, VCust can be configured to
bounce the message, reply to the message, or click links in the
message.  It is generally used to perform functional and load testing
of Brick Street Connect, and is provided to Connect customers under
their existing Connect software license.

The VCust program is provided as a single self-contained jar file:
robocust.jar.  To invoke it:

		java -jar robocust.jar

The program looks for a file named robocust.properties in the current
directory.  The robocust.properties file configures all facets of the
VCust program.  Copy robocust.properties.template to
robocust.properties, and edit it for your test scenario. Read the
robocust.properties.template file for more information about
configuring the robocust.properties file.

Note: Virtual Customer and Robocust will probably be used
interchangeably as names for this technology.

