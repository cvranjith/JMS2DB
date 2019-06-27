import java.util.Properties;
import java.sql.*;
import javax.jms.*;

import java.util.Date;
import java.sql.Timestamp;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.naming.NamingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.sql.DataSource;

public class JMS2DB implements MessageListener
{
private Destination destination = null;
private JMSConsumer consumer = null;
private java.sql.Connection conn;


	private QueueConnectionFactory inJmsQConFactory;
	private QueueConnection inJmsQCon;
	private QueueSession inJmsQSession;
	private QueueReceiver inQReceiver;
	private Queue inJmsQueue; 
	
	
	private QueueConnectionFactory outJmsQConFactory;
	private QueueConnection outJmsQCon;
	private QueueSession outJmsQSession;
	private QueueSender outJmsQSender;
	private Queue outJmsQueue;

	
	private static Properties propf = new Properties();
	
 public void onMessage(Message msg)
 {
    try {
     String msgText;
     if (msg instanceof TextMessage) {
       msgText = ((TextMessage)msg).getText();
     } else {
       msgText = msg.toString();
     }
	log("*********************");
    log ("Message Received from Queue: " + msgText );
	 
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
 
 public static String val(String param){
		return (String)propf.get(param);
	}
 public void DBinit() throws SQLException, NamingException
 {
        DataSource ds=null;
        Context ctx=null;
        Hashtable ht = new Hashtable();
        ht.put(Context.INITIAL_CONTEXT_FACTORY,"weblogic.jndi.WLInitialContextFactory");
		ht.put(Context.PROVIDER_URL, val("WLS_PROVIDER_URL"));
		ctx=new InitialContext(ht);
        ds=(DataSource)ctx.lookup(val("WLS_DB_DATASOURCE"));
		conn=ds.getConnection();
		log ("DB Connection established");
  }
  
	public void WLSinit() throws NamingException, JMSException{
		Hashtable env = new Hashtable();
		JMSContext context = null;
		env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
		env.put(Context.PROVIDER_URL, val("WLS_PROVIDER_URL"));
		InitialContext ic = new InitialContext(env);
		inJmsQConFactory = (QueueConnectionFactory) ic.lookup(val("WLS_FACTORY_NAME"));
		inJmsQCon = inJmsQConFactory.createQueueConnection();
		inJmsQSession = inJmsQCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		inJmsQueue = (Queue) ic.lookup(val("WLS_IN_QUEUE_NAME"));
		inQReceiver = inJmsQSession.createReceiver(inJmsQueue);
		inQReceiver.setMessageListener(this);
		inJmsQCon.start();
		log("Started JMS connection for Listening to " + val("WLS_IN_QUEUE_NAME") );
		
		
		outJmsQConFactory = (QueueConnectionFactory) ic.lookup(val("WLS_FACTORY_NAME"));
		outJmsQCon = outJmsQConFactory.createQueueConnection();
		outJmsQSession = outJmsQCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		outJmsQueue = (Queue) ic.lookup(val("WLS_OUT_QUEUE_NAME"));
		outJmsQSender = outJmsQSession.createSender(outJmsQueue);
		outJmsQCon.start();
		log("Started JMS connection for sending to " + val("WLS_OUT_QUEUE_NAME") );
		
	}
 
 private void send2DB(String msg,String msgprops) throws SQLException,JMSException
 {
	log("Calling DB");
	CallableStatement callSt = conn.prepareCall("{ call "+val("DB_PROC")+"(?,?,?) }");
	callSt.setString(1, msg);
	callSt.setString(2, msgprops);
	callSt.registerOutParameter(3, java.sql.Types.VARCHAR);
	callSt.execute();
	String respMsg = callSt.getString(3);
	callSt.close();
	log("DB Response Received: "+respMsg);
	sendResponse(respMsg);
 }

 private void sendResponse(String msg) throws JMSException
 {
	TextMessage txtMsg;
	txtMsg = outJmsQSession.createTextMessage();
	txtMsg.setText(msg);
	outJmsQSender.send(txtMsg);
	log("Response Sent to Queue:");
	log("########################");
 }
 private void log(String msg)
 {
 System.out.println("["+(new Timestamp(new Date().getTime()))+"] "+msg);
 }
 public static void main(String argv[]) throws JMSException, SQLException, InterruptedException,FileNotFoundException,IOException,NamingException
 {
    JMS2DB JMS2DB = new JMS2DB();
	propf.load(new BufferedInputStream(new FileInputStream(new File(argv[0]))));
    JMS2DB.DBinit();
	JMS2DB.WLSinit();
	synchronized(JMS2DB) {
         JMS2DB.wait();
    }
  }
}



