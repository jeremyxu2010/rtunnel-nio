package com.cloudbility.rtunnel.client;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

import com.cloudbility.rtunnel.common.ReceivingDataAsPacket;
import com.cloudbility.rtunnel.common.WritePacketDataHandler;

/**
 * 连接proxy server的ChannelPipelineFactory
 * @author atlas
 * @date 2012-11-1
 */
public class ClientOutboundChannelPipelineFactory implements
		ChannelPipelineFactory {
	
	@SuppressWarnings("unused")
	private ClientPipeConfig config;
	
	public ClientOutboundChannelPipelineFactory(ClientPipeConfig config) {
		super();
		this.config = config;
	}


	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline cpl = Channels.pipeline();
		//把Packet的数据写入流
		cpl.addLast("writeDecoder", new WritePacketDataHandler());
		//把读出的数据封装成Packet
		cpl.addLast("readDecoder", new ReceivingDataAsPacket());
		
		//
		ClientOutboundHandler handler = new ClientOutboundHandler();
		cpl.addLast("outboundHandler", handler);
		return cpl;
	}
	 
}
