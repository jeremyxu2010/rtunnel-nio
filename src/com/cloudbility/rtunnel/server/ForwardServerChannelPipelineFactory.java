package com.cloudbility.rtunnel.server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

import com.cloudbility.rtunnel.common.ReceivingDataAsPacket;
import com.cloudbility.rtunnel.common.WritePacketDataHandler;

/**
 * forward server accept a channel from forward client
 * @author atlas
 * @date 2012-11-1
 */
public class ForwardServerChannelPipelineFactory implements
		ChannelPipelineFactory {
	private RTunnelServerBootstrap server;
	private ForwardServer info;

	public ForwardServerChannelPipelineFactory(RTunnelServerBootstrap server,
			ForwardServer info) {
		super();
		this.server = server;
		this.info = info;
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline cpl = Channels.pipeline();
		//wrap whatever to a Packet
		cpl.addLast("packetDecoder", new ReceivingDataAsPacket());
		//if writing a packet, just write it's data area.
		cpl.addLast("packetEncoder", new WritePacketDataHandler());
		
		cpl.addLast("uncompressPacket", new UncompressPacketHandler());
		//
		cpl.addLast("forwarder", new ForwardHandler(server, info));
		return cpl;
	}
}
