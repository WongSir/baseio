package com.yoocent.mtp.jms.server;

import com.yoocent.mtp.jms.ErrorMessage;
import com.yoocent.mtp.jms.Message;
import com.yoocent.mtp.jms.NullMessage;
import com.yoocent.mtp.server.MTPServlet;
import com.yoocent.mtp.server.Request;
import com.yoocent.mtp.server.Response;
import com.yoocent.mtp.server.session.Session;

public class JMSConsumerServlet extends MTPServlet{

	public static String SERVICE_NAME = JMSConsumerServlet.class.getSimpleName();
	
	public void accept(Request request, Response response) throws Exception {

		
		Session session = request.getSession();
		
		MQContext context = MQContextFactory.getMQContext();
		
		if (context.isLogined(session)) {
			
			long timeout = request.getLongParameter("timeout");
			
			Message message = context.pollMessage(request, timeout);
			
			if (session.connecting()) {

				if (message == null) {
					message = NullMessage.NULL_MESSAGE;
				}
				
				String content = message.toString();
				
				response.write(content.getBytes());
				
				response.flush();
				
			}else{
				if (message != null) {
					
					context.regist(message);
				}
			}
			
		}else{
			Message message = ErrorMessage.UNAUTH_MESSAGE;
			response.write(message.toString());
			response.flush();
			
		}
	}
}
