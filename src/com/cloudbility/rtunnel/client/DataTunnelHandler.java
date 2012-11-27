package com.cloudbility.rtunnel.client;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.CommonHandler;
import com.cloudbility.rtunnel.buffer.Packet;

/**
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class DataTunnelHandler extends CommonHandler {
	private Logger logger = LoggerFactory.getLogger(DataTunnelHandler.class);

	// agentd-proxyServer
	private Channel outboundChannel;

	private int forwardInfo;

	public DataTunnelHandler(Channel outboundChannel, int forwardInfo) {
		this.outboundChannel = outboundChannel;
		this.forwardInfo = forwardInfo;
	}

	// send the first packet in data tunnel to identify itself.
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		Packet p = new Packet(8);
		p = Packet.fillACKNewTcpSocketPacket(p, forwardInfo);
		if (logger.isDebugEnabled())
			logger.debug("write ACK_NEW_TCP_SOCKET packet " + p);
		e.getChannel().write(p.wrapPacket());
	}

	// pipe whatever received to outboundChannel
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		outboundChannel.write(e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		logger.error("error of data tunnel " + forwardInfo, e.getCause());
		e.getChannel().close();
	}

	@Override
	protected Logger log() {
		return logger;
	}

}
