import java.util.Properties;
import java.sql.*;
import javax.jms.*;
import oracle.jms.*;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import java.util.Date;
import java.sql.Timestamp;


public class ORA2MQ implements MessageListener
{
private QueueReceiver qreceiver;
private QueueSession qsess;
private QueueConnection qconn;
private Queue q;
private java.sql.Connection conn;

private static final String MQ_HOST = "localhost";
private static final int MQ_PORT = 1414;
private static final String MQ_CHANNEL = "DEV.APP.SVRCONN";
private static final String MQ_QMGR = "QM1";
private static final String MQ_APP_USER = "app";
private static final String MQ_APP_PASSWORD = "Example!1";
private static final String MQ_QUEUE_NAME = "DEV.QUEUE.1";

private static final String DB_URL = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=oradb.sg.oracle.com)))";
private static final String DB_UID = "FCUBS_REST";
private static final String DB_PWD = "Oracle123";
private static final String DB_AQ_QUEUE = "Q";

private JMSContext context = null;
private Destination destination = null;
private JMSProducer producer = null;	

 public void onMessage(Message msg)
 {
    try {
     String msgText;
     if (msg instanceof TextMessage) {
       msgText = ((TextMessage)msg).getText();
     } else {
       msgText = msg.toString();
     }
     System.out.println("Message Received: " + (new Timestamp(new Date().getTime())) + " : " + msgText );
	 producer.send(destination, msgText);
	 System.out.println("Sent to MQ:" + msgText);
	 qsess.commit();
    } catch (JMSException e) {
     System.err.println("An exception occurred: "+e.getMessage());
	 e.printStackTrace();
    }
 }
 
 public void close()throws JMSException
 {
    qreceiver.close();
    qsess.close();
    qconn.close();
 }
 public void init() throws SQLException, JMSException
 {
	 
	JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
	JmsConnectionFactory cf = ff.createConnectionFactory();
	cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, MQ_HOST);
	cf.setIntProperty(WMQConstants.WMQ_PORT, MQ_PORT);
	cf.setStringProperty(WMQConstants.WMQ_CHANNEL, MQ_CHANNEL);
	cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
	cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, MQ_QMGR);
	cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "ORA2MQ");
	cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
	cf.setStringProperty(WMQConstants.USERID, MQ_APP_USER);
	cf.setStringProperty(WMQConstants.PASSWORD, MQ_APP_PASSWORD);
	context = cf.createContext();
	destination = context.createQueue("queue:///" + MQ_QUEUE_NAME);
	producer = context.createProducer();

	Properties props = new Properties();
	props.setProperty("user", DB_UID);
	props.setProperty("password", DB_PWD);
	DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
	conn = DriverManager.getConnection(DB_URL, props);
	qconn = AQjmsQueueConnectionFactory.createQueueConnection(conn);
	qsess = qconn.createQueueSession(true, 0);
	q = qsess.createQueue(DB_AQ_QUEUE);
	qreceiver = qsess.createReceiver(q);
	qreceiver.setMessageListener(this);
	qconn.start();
 }
 
public static void main(String argv[]) throws JMSException,SQLException,InterruptedException
 {
    ORA2MQ ora2mq = new ORA2MQ();
    ora2mq.init();
	synchronized(ora2mq) {
         ora2mq.wait();
    }
    ora2mq.close();
 }
}