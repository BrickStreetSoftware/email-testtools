package brickst.robocust.vc;

import brickst.robocust.lib.SystemConfig;
import brickst.robocust.smtp.SimpleEmailMessage;
import brickst.robocust.smtp.SmtpMessage;

/**
 * @author Catalin Petrisor (cpetrisor@brickstreetsoftware.com)
 */
public class UnsubscribeTester extends DefaultReceiverTester {
  private String sender = SystemConfig.getInstance().getProperty(SystemConfig.VC_SENDER_ADDRESS);

  public String handle(SmtpMessage msg) {
      // unsubscribe.
      String body = "\n\nunsubscribe\n\n" +
        "***********original message ************\r\n" +
        new String(msg.getData()); // + ttsr.message;
      SimpleEmailMessage message = setUpMessage(msg.getEnvelopeSender(), body);
      message.setSubject("am not interested");
      boolean status = send(message);
      return (status ? "unsubscribe" : "unsubscribe error");
  }
}
