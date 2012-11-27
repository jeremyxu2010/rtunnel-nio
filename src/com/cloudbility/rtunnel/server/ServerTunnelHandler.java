package com.cloudbility.rtunnel.server;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.buffer.Packet;
import com.cloudbility.rtunnel.common.CommonHandler;

/**
 * 控制通道或者数据通道，取决于收到的第一个Packet是否是ACK_NEW_TCP_SOCKET
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class ServerTunnelHandler extends CommonHandler {
	private static Logger logger = LoggerFactory
			.getLogger(ServerTunnelHandler.class);
	/**
	 * default is true
	 */
	private boolean serveControl = true;

	private RTunnelServerBootstrap serverBootstrap;

	private int forwardPort = -1;

	/**
	 * 数据通道才具有
	 */
	private Channel forwardChannel;

	public ServerTunnelHandler(RTunnelServerBootstrap serverBootstrap) {
		super();
		this.serverBootstrap = serverBootstrap;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Packet packet = (Packet) e.getMessage();
		if (serveControl) {
			Packet p = packet;

			if (p.isProtocol(Packet.TCP_SERVER_PORT)) {
				// a control tunnel forever.
				if (logger.isDebugEnabled()) {
					logger.debug("received TCP_SERVER_PORT packet " + p);
				}
				boolean ok = false;
				try {
					forwardPort = p.extractInt();
					ForwardConfig config = new ForwardConfig();
					config.setClientChannel(ctx.getChannel());
					config.setForwardPort(forwardPort);
					ok = serverBootstrap.newForwardServer(config);
					p = Packet.fillACKTcpServerPortPacket(p, ok ? 0 : 1);
					if (logger.isDebugEnabled()) {
						if (ok) {
							logger.debug("write success ACK_TCP_SERVER_PORT packet "
									+ p);
						} else {
							logger.debug("already bound.Write failed ACK_TCP_SERVER_PORT packet "
									+ p);
						}
					}
				} catch (Exception error) {
					logger.error("error when establish forward server on port "
							+ forwardPort, error);
					p = Packet.fillACKTcpServerPortPacket(p, 1);
					if (logger.isDebugEnabled()) {
						logger.debug("IOError,write fail ACK_TCP_SERVER_PORT packet "
								+ p);
					}
					ok = false;
				}
				// send ack
				ChannelFuture future = e.getChannel().write(p);
				if (ok == false) {// close it after ack
					future.addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future)
								throws Exception {
							// close this control channel
							future.getChannel().close();
						}
					});
				}

			} else if (p.isProtocol(Packet.ACK_NEW_TCP_SOCKET)) {
				if (logger.isDebugEnabled()) {
					logger.debug("received ACK_NEW_TCP_SOCKET packet " + p);
				}
				// oh,this is a data tunnel with first packet a
				// ACK_NEW_TCP_SOCKET packet.
				// forward server is established
				serveControl = false;
				int bindInfo = p.extractInt();
				int forwardServerPort = (int) (bindInfo >>> 16);
				int forwardClientId = (int) (bindInfo & 0xffff);
				if (logger.isDebugEnabled()) {
					logger.debug(
							"received ACK_NEW_TCP_SOCKET packet forward server port={}, forward client id={}",
							forwardServerPort, forwardClientId);
				}
				this.serverBootstrap.newPipe(forwardServerPort,
						forwardClientId, this);
			} else if (p.isProtocol(Packet.CLOSE_TUNNEL)) {
				// close this control tunnel
				try {
					getChannel().close();
				} finally {
					this.serverBootstrap.onControlTunnelClosed(forwardPort);
				}
			}
		} else {
			// serve as data channel.
			forwardChannel.write(e.getMessage());
		}
	}

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		super.writeRequested(ctx, e);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		super.exceptionCaught(ctx, e);
	}

	/**
	 * 给数据通道设置forward通道，
	 * 
	 * @param forwardChannel
	 */
	public void setForwardChannel(Channel forwardChannel) {
		this.forwardChannel = forwardChannel;
		setupMutualCloseListener(this.forwardChannel);
	}

	@Override
	protected Logger log() {
		return logger;
	}

}
