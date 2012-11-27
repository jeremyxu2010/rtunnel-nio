package com.cloudbility.rtunnel.server;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

import com.cloudbility.rtunnel.HeartBeatTimerHandler;
import com.cloudbility.rtunnel.ReceivingPacketHandler;
import com.cloudbility.rtunnel.WritePacketHandler;

/**
 * rtunnel client first connected to rtunnel server, or rtunnel client connected
 * to server for a forward client.
 * 
 * @author atlas
 * @date 2012-11-1
 */
public class ServerInboundChannelPipelineFactory implements
		ChannelPipelineFactory {
	private RTunnelServerBootstrap server;

	public ServerInboundChannelPipelineFactory(RTunnelServerBootstrap server) {
		super();
		this.server = server;
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline cpl = Channels.pipeline();
		// read a whole packet
		cpl.addLast("readWholePacket", new ReceivingPacketHandler());
		
		// write a whole packet
		cpl.addLast("writeWholePacket", new WritePacketHandler());

		// filter heart beat packet
		cpl.addLast("heartBeatFilter", new HeartBeatTimerHandler());
		
		// no heartbeat packet will reach the following handler
		// any data or control packet can reach here
		// serve as control or data tunnel
		cpl.addLast("controller", new ServerTunnelHandler(server));
		return cpl;
	}

}