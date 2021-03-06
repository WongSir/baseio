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
package com.generallycloud.nio.acceptor;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;

import com.generallycloud.nio.common.CloseUtil;
import com.generallycloud.nio.common.LifeCycleUtil;
import com.generallycloud.nio.component.DatagramChannelContext;
import com.generallycloud.nio.component.NioChannelService;
import com.generallycloud.nio.component.NioSocketChannelContext;
import com.generallycloud.nio.component.SelectorEventLoopGroup;
import com.generallycloud.nio.component.SocketSelectorBuilder;
import com.generallycloud.nio.component.SocketSelectorEventLoopGroup;
import com.generallycloud.nio.configuration.ServerConfiguration;
import com.generallycloud.nio.protocol.ReadFuture;

public final class DatagramChannelAcceptor extends AbstractChannelAcceptor implements NioChannelService{

	private DatagramChannelContext	context				= null;

	private DatagramSocket			datagramSocket			= null;

	private SelectableChannel		selectableChannel		= null;

	private SocketSelectorBuilder		selectorBuilder		= null;

	private SelectorEventLoopGroup	selectorEventLoopGroup	= null;

	public DatagramChannelAcceptor(DatagramChannelContext context) {
		this.selectorBuilder = new ServerNioSocketSelectorBuilder();
		this.context = context;
	}

	@Override
	protected void bind(InetSocketAddress socketAddress) throws IOException {
		
		this.initChannel();

		try {
			// 进行服务的绑定
			datagramSocket.bind(socketAddress);
		} catch (BindException e) {
			throw new BindException(e.getMessage() + " at " + socketAddress.getPort());
		}

		initSelectorLoops();
	}

	private void initSelectorLoops() {

		//FIXME socket selector event loop ?
		ServerConfiguration configuration = getContext().getServerConfiguration();

		int core_size = configuration.getSERVER_CORE_SIZE();

		int eventQueueSize = configuration.getSERVER_IO_EVENT_QUEUE();

		this.selectorEventLoopGroup = new SocketSelectorEventLoopGroup(
				(NioSocketChannelContext) getContext(), "io-process", eventQueueSize,
				core_size);
		LifeCycleUtil.start(selectorEventLoopGroup);
	}

	@Override
	public void broadcast(final ReadFuture future) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DatagramChannelContext getContext() {
		return context;
	}

	@Override
	public SocketSelectorBuilder getSelectorBuilder() {
		return selectorBuilder;
	}

	@Override
	public SelectableChannel getSelectableChannel() {
		return selectableChannel;
	}
	
	/* (non-Javadoc)
	 * @see com.generallycloud.nio.component.AbstractChannelService#destroyService()
	 */
	@Override
	protected void destroyService() {
		CloseUtil.close(datagramSocket);
		CloseUtil.close(selectableChannel);
		LifeCycleUtil.stop(selectorEventLoopGroup);
	}
	
	private void initChannel() throws IOException {
		// 打开服务器套接字通道
		this.selectableChannel = DatagramChannel.open();
		// 服务器配置为非阻塞
		this.selectableChannel.configureBlocking(false);

		this.datagramSocket = ((DatagramChannel) this.selectableChannel).socket();
	}

}
