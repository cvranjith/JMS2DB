import java.util.Properties;
import java.sql.*;
import javax.jms.*;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import java.util.Date;
import java.sql.Timestamp;

import java.util.Enumeration;


public class MQ2ORA implements MessageListener
{

private JMSContext context = null;
private Destination destination = null;
private JMSConsumer consumer = null;
private java.sql.Connection conn;

private static final String MQ_HOST = "localhost";
private static final int    MQ_PORT = 1414;
private static final String MQ_CHANNEL = "DEV.APP.SVRCONN";
private static final String MQ_QMGR = "QM1";
private static final String MQ_APP_USER = "app";
private static final String MQ_APP_PASSWORD = "Example!1";
private static final String MQ_QUEUE_NAME = "DEV.QUEUE.1";

private static final String DB_URL = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=localhost)(PORT=1521))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=oradb.sg.oracle.com)))";
private static final String DB_UID = "FCUBS_REST";
private static final String DB_PWD = "Oracle123";
	
 public void onMessage(Message msg)
 {
    try {
     String msgText;
     if (msg instanceof TextMessage) {
       msgText = ((TextMessage)msg).getText();
     } else {
       msgText = msg.toString();
     }
     System.out.println("Message Received: "+ (new Timestamp(new Date().getTime())) + " " + msgText );
	 
	String msgProperties = "{\n";
	 
	String propName;
	String sep = "";
	Enumeration<String> props = msg.getPropertyNames();
	if (props != null)
	{
	   while (props.hasMoreElements())
	   {
		  propName = props.nextElement();
	      msgProperties = msgProperties+sep+"\""+propName+"\" : \""+msg.getObjectProperty(propName)+"\"";
		  sep=",\n";
	   }
	}
	msgProperties = msgProperties+"\n}";
	
	send2DB(msgText,msgProperties);
		 
    } catch (JMSException | SQLException e) {
     System.err.println("An exception occurred: "+e.getMessage());
	 e.printStackTrace();
    }
 }
 public void init(MQ2ORA mq2ora) throws JMSException,SQLException
 {
		try {
			JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
			JmsConnectionFactory cf = ff.createConnectionFactory();
			cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, MQ_HOST);
			cf.setIntProperty(WMQConstants.WMQ_PORT, MQ_PORT);
			cf.setStringProperty(WMQConstants.WMQ_CHANNEL, MQ_CHANNEL);
			cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
			cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, MQ_QMGR);
			cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "MQ2ORA");
			cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
			cf.setStringProperty(WMQConstants.USERID, MQ_APP_USER);
			cf.setStringProperty(WMQConstants.PASSWORD, MQ_APP_PASSWORD);
			context = cf.createContext();
			destination = context.createQueue("queue:///" + MQ_QUEUE_NAME);
			consumer = context.createConsumer(destination);
			consumer.setMessageListener(mq2ora);
			
			Properties props = new Properties();
			props.setProperty("user", DB_UID);
			props.setProperty("password", DB_PWD);
			DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
			conn = DriverManager.getConnection(DB_URL, props);
		} catch (JMSException e) {
			e.printStackTrace();
		}
  }

 private void send2DB(String msg,String msgprops) throws SQLException
 {
	System.out.println("DBsend "+ (new Timestamp(new Date().getTime())) + " " + msg );
	CallableStatement callSt = conn.prepareCall("{ call pr_process_msg(?,?) }");
	callSt.setString(1, msg);
	callSt.setString(2, msgprops);
	callSt.execute();
	callSt.close();
 }

 public static void main(String argv[]) throws JMSException, SQLException, InterruptedException
 {
    MQ2ORA mq2ora = new MQ2ORA();
    mq2ora.init(mq2ora);
	synchronized(mq2ora) {
         mq2ora.wait();
    }
  }
}