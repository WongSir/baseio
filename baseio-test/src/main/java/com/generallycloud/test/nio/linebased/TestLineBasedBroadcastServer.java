/*
 * Copyright 2015-2017 GenerallyCloud.com
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.generallycloud.test.nio.linebased;

import com.generallycloud.nio.acceptor.ChannelAcceptor;
import com.generallycloud.nio.acceptor.SocketChannelAcceptor;
import com.generallycloud.nio.codec.linebased.LineBasedProtocolFactory;
import com.generallycloud.nio.component.IoEventHandleAdaptor;
import com.generallycloud.nio.component.LoggerSocketSEListener;
import com.generallycloud.nio.component.NioSocketChannelContext;
import com.generallycloud.nio.component.NioSocketChannelContext;
import com.generallycloud.nio.component.SocketSession;
import com.generallycloud.nio.configuration.ServerConfiguration;
import com.generallycloud.nio.protocol.ReadFuture;

public class TestLineBasedBroadcastServer {

	public static void main(String[] args) throws Exception {

		IoEventHandleAdaptor eventHandleAdaptor = new IoEventHandleAdaptor() {
			
			@Override
			public void accept(SocketSession session, ReadFuture future) throws Exception {
				
				long old = System.currentTimeMillis();
				
				String res = "hello world!";
				
				future.write(res);
				
				ChannelAcceptor acceptor = (ChannelAcceptor) session.getContext().getChannelService();
				
				acceptor.broadcast(future);
				
				long now = System.currentTimeMillis();
				
				System.out.println("广播花费时间："+(now - old)+",连接数："+session.getContext().getSessionManager().getManagedSessionSize());
			}
		};

		ServerConfiguration configuration = new ServerConfiguration();
		
		configuration.setSERVER_PORT(18300);
		
		configuration.setSERVER_SESSION_IDLE_TIME(180000);
		
		configuration.setSERVER_MEMORY_POOL_CAPACITY(1024 * 512);
		
		configuration.setSERVER_MEMORY_POOL_UNIT(64);
		
		NioSocketChannelContext context = new NioSocketChannelContext(configuration);
		
		SocketChannelAcceptor acceptor = new SocketChannelAcceptor(context);
		
		context.addSessionEventListener(new LoggerSocketSEListener());
		
		context.setIoEventHandleAdaptor(eventHandleAdaptor);
		
		context.setProtocolFactory(new LineBasedProtocolFactory());

		acceptor.bind();
	}
}
