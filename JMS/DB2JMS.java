
import java.util.Properties;
import java.sql.*;
import javax.jms.*;
import oracle.jms.*;
import java.util.Date;
import java.sql.Timestamp;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DB2JMS implements MessageListener {
	private QueueReceiver aqreceiver;
	private QueueSession aqsession;
	private QueueConnection aqcon;
	private Queue aqqueue;

	private QueueConnectionFactory jmsqconFactory;
	private QueueConnection jmsqcon;
	private QueueSession jmsqsession;
	private QueueSender jmsqsender;
	private Queue jmsqueue;

	private static Properties propf = new Properties();

	public void onMessage(Message msg){
		try {
			String msgText;
			TextMessage txtMsg;
			if (msg instanceof TextMessage) {
				msgText = ((TextMessage)msg).getText();
			} else {
				msgText = msg.toString();
			}
			System.out.println("Message Received: " + (new Timestamp(new Date().getTime())) + " : " + msgText );
			txtMsg = jmsqsession.createTextMessage();
			txtMsg.setText(msgText);
			jmsqsender.send(msg);

			System.out.println("Sent to MQ:" + msgText);
			aqsession.commit();
			} catch (JMSException e) {
				System.err.println("An exception occurred: "+e.getMessage());
				e.printStackTrace();
			}
	}
	public void close()throws JMSException{
		aqreceiver.close();
		aqsession.close();
		aqcon.close();
		jmsqsender.close();
		jmsqsession.close();
		jmsqcon.close();
	}
	public static String val(String param){
		return (String)propf.get(param);
	}
	public void DBinit() throws SQLException, JMSException{
		java.sql.Connection conn;
		Properties props = new Properties();
		props.setProperty("user", val("DB_UID"));
		props.setProperty("password", val("DB_PWD"));
		DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
		conn = DriverManager.getConnection(val("DB_URL"), props);
		aqcon = AQjmsQueueConnectionFactory.createQueueConnection(conn);
		aqsession = aqcon.createQueueSession(true, 0);
		aqqueue = aqsession.createQueue(val("DB_AQ_QUEUE_NAME"));
		aqreceiver = aqsession.createReceiver(aqqueue);
		aqreceiver.setMessageListener(this);
		aqcon.start();
		System.out.println("Connected to DB");
	}
	public void WLSinit() throws NamingException, JMSException{
		Hashtable env = new Hashtable();
		JMSContext context = null;
		env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
		env.put(Context.PROVIDER_URL, val("WLS_PROVIDER_URL"));
		InitialContext ic = new InitialContext(env);
		jmsqconFactory = (QueueConnectionFactory) ic.lookup(val("WLS_FACTORY_NAME"));
		jmsqcon = jmsqconFactory.createQueueConnection();
		jmsqsession = jmsqcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		jmsqueue = (Queue) ic.lookup(val("WLS_OUT_QUEUE_NAME"));
		jmsqsender = jmsqsession.createSender(jmsqueue);
		jmsqcon.start();
		System.out.println("Connected to JMS");
	}
	public static void main(String argv[]) throws JMSException,SQLException,InterruptedException,NamingException,FileNotFoundException,IOException{
		DB2JMS DB2JMS = new DB2JMS();
		propf.load(new BufferedInputStream(new FileInputStream(new File(argv[0]))));
		DB2JMS.DBinit();
		DB2JMS.WLSinit();
		synchronized(DB2JMS) {
			DB2JMS.wait();
		}
		DB2JMS.close();
	}
}


