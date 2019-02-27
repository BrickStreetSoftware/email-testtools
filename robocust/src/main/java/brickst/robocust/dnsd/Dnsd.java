/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.dnsd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.log4j.Logger;


/**
 * Virtual DNS Deamon Server.<br>
 * The server blocks on an UDP socket on a port (usually 53), which is doing "receive".<br>
 * The server will dispach the incoming data to a handler
 * which will handle the DNS request and return the coresponding response.
 * The server will send the response back to the client.
 * <p>
 * To use this class, you need to do the following:
 * <li>Instantiate an object that implements the DnsdRequestHandler interface.
 * <li>Instantiate a Dnsd object with the handler.
 * <p>
 * The sample usage should be:
 * <pre>
 * class DnsServer
 * {
 *    Dnsd dnsd;
 *    DnsServer()
 *    {
 *       dnsd = new Dnsd(53, this);
 *       dnsd.start();
 *	  }
 *    public DnsdResponse handle(DnsdRequest request)
 *    {
 *        DnsdResponse response = new DnsdResponse(request);
 *        String name = request.getName();
 *        int ttl = 3600;
 *
 *        switch (request.getType())
 *        {
 *           case DnsdRecord.TYPE_MX:
 *                int preference = 10;
 *                String mailExchange = "mail." + name;
 *                response.addAnswer(new DnsdMxRecord(name, ttl, preference, mailExchange));
 *                break;
 *
 *           case DnsdRecord.TYPE_A:
 *                String ipAddress = "<IP of this machine>";
 *                response.addAnswer(new DnsdARecord(name, ttl, ipAddress));
 *                break;
 *    }
 *
 *    return response;
 * }
 </pre>
 * @author Jing Zhou
 * @see DnsdRequestHandler
 */
public class Dnsd extends Thread
{
	private DatagramSocket socket;

	private DnsdRequestHandler handler;

	private static final int max = 512;
	static Logger logger = Logger.getLogger(Dnsd.class);


	/**
	 * Creates DNS daemon that will wait for UDP packets on the specified port
	 *
	 * @param port the port to wait on which normally is the well-known port for DNS - 53
	 * @param handler a call-back to the DNS request handler
	 */
	public Dnsd(int port, DnsdRequestHandler handler) throws SocketException
	{
		this.handler = handler;
		socket = new DatagramSocket(port);
	}

	/**
	 * Implements the execute in CRMThread.
	 * Keeps on handling the income request in while loop
	 */
	public void run()
	{
		while (true) {
			byte[] request = new byte[max];
			DatagramPacket packet = new DatagramPacket(request , request.length);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				//log and handle
				//use the ErrorMonitor
				//leave the handle for continues error to the frame work?
				logger.error("Dnsd: IOException at UDP socket receive.", e);
				continue;
			}
			DnsdByteParser parser = new DnsdByteParser(packet.getData(), packet.getLength());
			DnsdByteBuilder builder = handle(parser);

			InetAddress address = packet.getAddress();
			int port = packet.getPort();
			packet = new DatagramPacket(builder.getBytes(), builder.getCurrentPosition(), address, port);
			try {
				socket.send(packet);
			} catch (IOException e) {
				logger.error("Dnsd: IOException at UDP socket send." , e);
				continue;
			}
		}
	}

	/**
	 * Calls the handler to handle the request.
	 * @param parser the parser holds the byte array of request.
	 * @return the builder which holds the byte array of the response.
	 */
	private DnsdByteBuilder handle(DnsdByteParser parser)
	{
		DnsdRequest request = new DnsdRequest(parser);
		logger.info("Dnsd INFO: the Request is: ID: " + request.getHeader().getId());
		DnsdResponse response = handler.handle(request);
		logger.info("Dnsd INFO: the Response is: ID: " + response.getHeader().getId());
		DnsdByteBuilder builder = new DnsdByteBuilder();
		response.pack(builder);
		return builder;
	}
}
