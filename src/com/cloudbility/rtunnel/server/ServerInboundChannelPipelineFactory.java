package com.cloudbility.rtunnel.server;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

import com.cloudbility.rtunnel.common.CipherHolder;
import com.cloudbility.rtunnel.common.DHKeyExchangerHandler;
import com.cloudbility.rtunnel.common.DataPacketProcessorHandler;
import com.cloudbility.rtunnel.common.EncryptPacketHandler;
import com.cloudbility.rtunnel.common.HeartBeatTimerHandler;
import com.cloudbility.rtunnel.common.ReceivingPacketHandler;
import com.cloudbility.rtunnel.common.WritePacketHandler;

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
		
		AtomicBoolean compressed = new AtomicBoolean(false);
		
		cpl.addLast("retrieveCompressFlag", new RetrieveCompressFlagHandler(compressed));
		
		CipherHolder cipherHolder = new CipherHolder();
		cpl.addLast("DHKeyExchanger", new DHKeyExchangerHandler(cipherHolder));
		cpl.addLast("dataPacketProcessor", new DataPacketProcessorHandler(cipherHolder));
		
		cpl.addLast("compressPacket", new CompressPacketHandler(compressed));
		cpl.addLast("encryptPacket", new EncryptPacketHandler(cipherHolder));
		
		// no heartbeat packet will reach the following handler
		// any data or control packet can reach here
		// serve as control or data tunnel
		cpl.addLast("controller", new ServerTunnelHandler(server));
		return cpl;
	}

}
