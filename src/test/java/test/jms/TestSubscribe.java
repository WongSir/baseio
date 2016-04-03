package test.jms;

import java.io.IOException;

import test.ClientUtil;

import com.gifisan.nio.client.ClientConnector;
import com.gifisan.nio.client.ClientSesssion;
import com.gifisan.nio.jms.JMSException;
import com.gifisan.nio.jms.Message;
import com.gifisan.nio.jms.client.MessageConsumer;
import com.gifisan.nio.jms.client.impl.MessageConsumerImpl;

public class TestSubscribe {

	public static void main(String[] args) throws IOException, JMSException {

		
		for (int i = 0; i < 5; i++) {
			
			new Thread(new Runnable() {
				
				public void run() {
					try {
						TestSubscribe.test();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
			
		}
		
	}

	private static void test() throws Exception {

		ClientConnector connector = ClientUtil.getClientConnector();

		connector.connect();

		ClientSesssion session = connector.getClientSession();

		MessageConsumer consumer = new MessageConsumerImpl(session, "qName");

		consumer.login("admin", "admin100");

		Message message = consumer.subscibe();

		System.out.println(message);

		consumer.logout();

		connector.close();
	}

}
