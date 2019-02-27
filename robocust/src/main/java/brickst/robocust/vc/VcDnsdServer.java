/*
 * Copyright (c) 2000 by Kana Communications, Inc. All Rights Reserved.
 */

package brickst.robocust.vc;

import brickst.robocust.dnsd.*;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;

/**
 * VcDnsdServer will do the following:
 * <li>For A type query: return the ip of the VC.
 * <li>For MX type query: return mail.domain
 * <li>For any other type of query: return a response without any answer in it.
 *
 * @author Jing Zhou
 */
//Develbox: task_24_multiple_CM_clusters
public class VcDnsdServer implements DnsdRequestHandler
{
	static Logger log = Logger.getLogger(VcDnsdServer.class);
	
	protected Properties zone;
	protected int port = 53;
	protected ArrayList<String> machineList;
	protected Dnsd dnsServer;
	
	/**
	 * Creates the VcDnsServer which will start the Dns deamon.
	 */
	public VcDnsdServer(Properties zonefile)
		throws SocketException
	{
		zone = zonefile;
		initZone();
	}

	public void start()
	{
		dnsServer.start();
	}
	
	protected void initZone()
		throws SocketException
	{
		String propval = zone.getProperty("dns.port");
		try {
			port = Integer.parseInt(propval);
		}
		catch (Exception x) {
			log.error("bad dns.port: " + propval, x);
			port = 53;
		}
		
		machineList = new ArrayList<String>();
		int cnt = 0;
		while (true) {
			propval = zone.getProperty("vcust_" + cnt);
			if (propval == null) {
				break;
			}
			machineList.add(propval);
			cnt++;
		}
		if (log.isInfoEnabled()) {
			log.info("Found " + cnt + " virtual customer servers");
		}
		
		dnsServer = new Dnsd(port, this);
	}
	
	/**
	 * Handles different type of Request.
	 * @param request a DnsdRequest object
	 * @return the DnsdResponse for the request
	 */
	public DnsdResponse handle(DnsdRequest request)
	{
		DnsdResponse response = new DnsdResponse(request);
		String name = request.getName();
		int ttl = 3600;
		
		switch (request.getType())
		{
			case DnsdRecord.TYPE_MX:
				int preference = 10;
				String mailExchange = "mail." + name;
				response.addAnswer(new DnsdMXRecord(name, ttl, preference, mailExchange));
				break;

			case DnsdRecord.TYPE_A:
				String ipAddress = null;
				try {
					// pick a server at random
					Random rnd = new Random();					
					int random = rnd.nextInt(machineList.size());
					String randomVC = machineList.get(random);
					ipAddress = InetAddress.getByName(randomVC).getHostAddress();

					if (ipAddress == null) {
						ipAddress = InetAddress.getLocalHost().getHostAddress();
					}
					response.addAnswer(new DnsdARecord(name, ttl, ipAddress));
				}
				catch (UnknownHostException e)
				{
					log.error("VcDnsdServer: Can't getLocalHost", e);
					//do something?
					response.addAnswer(new DnsdARecord(name, ttl, "127.0.0.1"));
				}
				break;

			// Respond to the initial Mail Sender startup reverse DNS lookup
			case DnsdRecord.TYPE_PTR:
				response.addAnswer(new DnsdPTRRecord(name, ttl, "mailsender"));
				break;
		}
		return response;
	}
}
