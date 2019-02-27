package brickst.robocust.vc;

import brickst.robocust.lib.SystemConfig;
import brickst.robocust.smtp.SimpleEmailMessage;
import brickst.robocust.smtp.SmtpMessage;

/**
 * @author Catalin Petrisor (cpetrisor@brickstreetsoftware.com)
 */
public class SpamFBLTester extends DefaultReceiverTester {
  private String sender = SystemConfig.getInstance().getProperty(SystemConfig.VC_SENDER_ADDRESS);

  public String handle(SmtpMessage msg) {
      // spam FBL.
      String body = "This is an email abuse report for an email message\n\n" +
        "***********original message ************\r\n" +
        new String(msg.getData()); // + ttsr.message;
      SimpleEmailMessage message = setUpMessage(msg.getEnvelopeSender(), body);
      message.setSubject("complaint about message from");
      boolean status = send(message);
      return (status ? "spamfbl" : "spamfbl error");
  }
}
