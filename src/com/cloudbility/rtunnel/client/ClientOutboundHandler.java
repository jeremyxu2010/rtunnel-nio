package com.cloudbility.rtunnel.client;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.CommonHandler;

/**
 * 
 * agentd => proxy server
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class ClientOutboundHandler extends CommonHandler {
	private static Logger logger = LoggerFactory
			.getLogger(ClientOutboundHandler.class);

	// data:agentd -> agentSrv
	private Channel inboundChannel;

	public ClientOutboundHandler() {
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		e.getChannel().setReadable(false);
	}

	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		inboundChannel.write(e.getMessage());
	}

	public void setInboundChannel(Channel inboundChannel) {
		this.inboundChannel = inboundChannel;
		setupMutualCloseListener(this.inboundChannel);
		getChannel().setReadable(true);
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		// TODO Auto-generated method stub
		super.writeRequested(ctx, e);
	}

	@Override
	protected Logger log() {
		return logger;
	}
}
