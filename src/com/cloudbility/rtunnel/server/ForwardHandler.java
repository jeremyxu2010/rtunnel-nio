package com.cloudbility.rtunnel.server;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.CommonHandler;
import com.cloudbility.rtunnel.buffer.Packet;

/**
 * when connected,send a NEW_TCP_SOCKET packet to agentd with forward-port and
 * remote port through control channel.
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class ForwardHandler extends CommonHandler {
	private static Logger logger = LoggerFactory
			.getLogger(ForwardHandler.class);
	/**
	 * agentd-agentSrv controll channel
	 */
	private Channel clientControlChannel;
	/**
	 * Rtunnel server 与 rtunnel client 之间的数据通道
	 */
	private Channel dataChannel;

	private ForwardServer forwardServer;

	private RTunnelServerBootstrap serverBootstrap;

	private int forwardClientId = -1;

	public ForwardHandler(RTunnelServerBootstrap serverBootstrap,
			ForwardServer forwardServer) {
		this.serverBootstrap = serverBootstrap;
		this.forwardServer = forwardServer;
		this.clientControlChannel = forwardServer.getConfig().getClientChannel();
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		// do not read it until dataChannel is ready ....
		ctx.getChannel().setReadable(false);
	}

	// 一个客户端连接到了AgentSrv上的 forward server,此时通过控制通道发送一个NEW_TCP_SOCKET包
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		Packet p = new Packet(8);
		forwardClientId = forwardServer.nextId();// remoteAddress.getPort();
		int forwardPort = forwardServer.getConfig().getForwardPort();
		serverBootstrap.addForwarClient(forwardServer.getConfig().getForwardPort(), this);
		int bindInfo = (int) ((int) (0xffff & forwardPort) << 16 | (int) (0xffff & forwardClientId) << 0);
		p = Packet.fillNewTcpSocketPacket(p, bindInfo);
		if (logger.isDebugEnabled()) {
			logger.debug("sending a NEW_TCP_SOCKET packet," + p
					+ ",forwardPort=" + forwardPort + ", forwardClientId="
					+ forwardClientId);
		}
		clientControlChannel.write(p);
		// do not read u
		e.getChannel().setReadable(false);
	}

	// message from forward-client
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		this.dataChannel.write(e.getMessage());
	}

	public int getForwardClientId() {
		return forwardClientId;
	}

	// 与agentd的数据通道准备好了，准备接收pipe的数据
	public void setDataChannel(Channel dataChannel) {
		this.dataChannel = dataChannel;
		setupMutualCloseListener(this.dataChannel);
		this.ctx.getChannel().setReadable(true);
	}

	@Override
	protected Logger log() {
		return logger;
	}

}
