/**
 * @(#)DefaultReceiverTester.java	1.0 00/1/31
 *
 * Copyright (c) 1999, 2000 by Kana Communications, Inc. All Rights Reserved.
 */
package brickst.robocust.vc;

import brickst.robocust.lib.*;
import brickst.robocust.smtp.*;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

abstract class DefaultReceiverTester
  implements ReceiverTester {
  static Logger logger = Logger.getLogger(DefaultReceiverTester.class);

  private double random;
  private VcCounter counter = new VcCounter();

  public VcCounter getCounter() {
    return counter;
  }

  public void setRandom(double random) {
    this.random = random;
  }

  public double getRandom() {
    return this.random;
  }

  public void getStatsReport(StringBuffer buff) {
    buff.append(getCounter().doReport());
  }

  public String getHandlerName() {
    return this.getClass().getName();
  }

  /**
   * this method will setup a simpleEmailMessage
   */
  protected SimpleEmailMessage setUpMessage(
    String recipient, String body) {
    SimpleEmailMessage message = new SimpleEmailMessage();
    message.setBody(body);
    message.setSmtpServer(SystemConfig.getInstance().getProperty(SystemConfig.VC_SMTP_SERVER));
    message.setSmtpPort(SystemConfig.getInstance().getIntProperty(SystemConfig.VC_SMTP_PORT));
    ArrayList<String> recps = new ArrayList<String>();
    recps.add(recipient);
    message.setRecipients(recps);

    //message.setSmtpPort(MailReceiverPort.getInstance().getValueNoCache());
    return message;
  }

  protected boolean send(SimpleEmailMessage msg) {
    try {
      // make sure msg is composed
      msg.writeTo(new ByteArrayOutputStream());

      msg.send();
      counter.increment();
      return true;
    } catch (IOException ex) {
      logger.debug(ex);
      return false;
    }
  }
}
