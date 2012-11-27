package com.cloudbility.rtunnel.client;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

import com.cloudbility.rtunnel.common.DHKeyExchangerHandler;
import com.cloudbility.rtunnel.common.HeartBeatTimerHandler;
import com.cloudbility.rtunnel.common.ReceivingPacketHandler;
import com.cloudbility.rtunnel.common.WritePacketHandler;

/**
 * rtunnel client to rtunnel server and for data tunnel
 * @author atlas
 * @date 2012-11-1
 */
public class ClientDataChannelPipelineFactory implements ChannelPipelineFactory {
	private ClientOutboundInfo info;
	private ClientPipeConfig config;

	public ClientDataChannelPipelineFactory(ClientOutboundInfo info,
			ClientPipeConfig config) {
		super();
		this.info = info;
		this.config = config;
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline cpl = Channels.pipeline();
		cpl.addLast("encoder", new ReceivingPacketHandler());
		
		cpl.addLast("decoder", new WritePacketHandler());
		
		// filter heart beat packet
		cpl.addLast("heartBeatTimer", new HeartBeatTimerHandler());
		
		cpl.addLast("DHKeyExchanger", new DHKeyExchangerHandler());
		
		// any data or control packet can reach here
		cpl.addLast(
				"inboundDataHandler",
				new DataTunnelHandler(info.getOutboundChannel(), config
						.getForwardInfo()));
		return cpl;
	}

}
