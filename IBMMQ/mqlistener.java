import java.util.Properties;
import java.sql.*;
import javax.jms.*;
//import oracle.jms.*;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import java.sql.*;
import java.sql.Connection;

public class mqlistener implements MessageListener
{

private JMSContext context = null;
private Destination destination = null;
private JMSConsumer consumer = null;

	private static final String HOST = "localhost"; // Host name or IP address
	private static final int PORT = 1414; // Listener port for your queue manager
	private static final String CHANNEL = "DEV.APP.SVRCONN"; // Channel name
	private static final String QMGR = "QM1"; // Queue manager name
	private static final String APP_USER = "app"; // User name that application uses to connect to MQ
	private static final String APP_PASSWORD = "Example!1";//"_APP_PASSWORD_"; // Password that the application uses to connect to MQ
	private static final String QUEUE_NAME = "DEV.QUEUE.1"; // Queue that the application uses to put and get messages to and from

		
private boolean quit = false;

 public void onMessage(Message msg)
 {
    try {
     String msgText;
     if (msg instanceof TextMessage) {
       msgText = ((TextMessage)msg).getText();
     } else {
       msgText = msg.toString();
     }
     System.out.println("Message Received: "+ msgText );
    } catch (JMSException jmse) {
     System.err.println("An exception occurred: "+jmse.getMessage());
    }
 }
 public void init(mqlistener jmsr)
    throws JMSException
 {
		try {
			// Create a connection factory
			JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
			JmsConnectionFactory cf = ff.createConnectionFactory();

			// Set the properties
			cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
			cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
			cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
			cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
			cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
			cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "JmsPutGet (JMS)");
			cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
			cf.setStringProperty(WMQConstants.USERID, APP_USER);
			cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);

			// Create JMS objects
			context = cf.createContext();
			destination = context.createQueue("queue:///" + QUEUE_NAME);

			consumer = context.createConsumer(destination); // autoclosable
			//String receivedMessage = consumer.receiveBody(String.class, 15000); // in ms or 15 seconds
			
			consumer.setMessageListener(jmsr);

			//System.out.println("\nReceived message:\n" + receivedMessage);

		} catch (JMSException jmsex) {
			jmsex.printStackTrace();
		}
 
 }
 
  public static void main(String argv[]) throws JMSException, SQLException {
    mqlistener jmsr = new mqlistener();
    jmsr.init(jmsr);
	
	synchronized(jmsr) {
     while (! jmsr.quit) {
       try {
         jmsr.wait();
       } catch (InterruptedException ie) {}
     }
    }
  }
}