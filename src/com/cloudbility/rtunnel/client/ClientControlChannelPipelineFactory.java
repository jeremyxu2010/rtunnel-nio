package com.cloudbility.rtunnel.client;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

import com.cloudbility.rtunnel.common.HeartBeatTimerHandler;
import com.cloudbility.rtunnel.common.ReceivingPacketHandler;
import com.cloudbility.rtunnel.common.WritePacketHandler;

/**
 * agentd->agentSrv 控制通道使用的ChannelPipelineFactory
 * @author atlas
 * @date 2012-11-1
 */
public class ClientControlChannelPipelineFactory implements
		ChannelPipelineFactory {
	private RTunnelClientBootstrap client;
	private ClientConfig config;

	public ClientControlChannelPipelineFactory(RTunnelClientBootstrap client,
			ClientConfig config) {
		super();
		this.client = client;
		this.config = config;
	}


	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline cpl = Channels.pipeline();
		//read a whole packet,控制协议包目前数据长度不超过8byte
		cpl.addLast("packetDecoder", new ReceivingPacketHandler(8));
		//write a whole packet
		cpl.addLast("packetEncoder", new WritePacketHandler());
		
		// filter heart beat packet
		cpl.addLast("heartBeatTimer", new HeartBeatTimerHandler());
		
		// any data or control packet can reach here
		cpl.addLast("controller", new ControlTunnelHandler(client, config));
		return cpl;
	}

}
