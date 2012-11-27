package com.cloudbility.rtunnel.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.common.CommonHandler;
import com.cloudbility.rtunnel.buffer.Packet;
import com.cloudbility.rtunnel.common.NetworkInterfaceHelper;

/**
 * 控制通道的handler，control handler:control tunnel for agentd->agnetsrv
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class ControlTunnelHandler extends CommonHandler {
	private static Logger logger = LoggerFactory
			.getLogger(ControlTunnelHandler.class);

	public static InetAddress LOCAL_INET_ADDRESS = null;

	static {
		try {
			LOCAL_INET_ADDRESS = InetAddress.getByName("0.0.0.0");
		} catch (UnknownHostException e) {
			// ignore exception
		}
	}

	private ClientConfig config;

	// ready if rtunnel server's forwardServer is ready
	private boolean ready = false;

	private RTunnelClientBootstrap client;

	private String rtunnelBindInterfaceName;
	private String DEFAULT_IP_VERSION = "ipv4";

	public ControlTunnelHandler(RTunnelClientBootstrap client,
			ClientConfig config) {
		super();
		this.client = client;
		this.config = config;
	}

	/**
	 * when connected,send the first packet to RTunnel server requesting for
	 * establishing a forward server.
	 */
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		final Packet cp = new Packet(8);
		Packet.fillTcpServerPortPacket(cp, config.getForwardPort(),
				config.getProxyServerPort());
		if (logger.isDebugEnabled())
			logger.debug("writing TCP_SERVER_PORT control packet " + cp);
		e.getChannel().write(cp);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Packet ctrlSegment = (Packet) e.getMessage();
		if (logger.isDebugEnabled()) {
			logger.debug("received control packet " + ctrlSegment);
		}
		if (ready) {
			messageReceivedAfterReady(ctx, ctrlSegment);
			return;
		}
		if (ctrlSegment.isProtocol(Packet.ACK_TCP_SERVER_PORT)) {
			if (ctrlSegment.getDataLen() > 0
					&& ctrlSegment.byteAt(0) == (byte) 0) {
				logger.info("the tunnel to transit server is ready.");
				ready = true;
			} else {
				logger.error("the tunnel with transit server is broken,or the forwardPort on rtunnel server is already in use.");
				client.stop();
				return;
			}
		}
	}

	/**
	 * after forward server is ready
	 * 
	 * @param ctx
	 * @param p
	 * @throws IOException
	 */
	private void messageReceivedAfterReady(ChannelHandlerContext ctx, Packet p)
			throws IOException {
		if (p.isProtocol(Packet.NEW_TCP_SOCKET)) {
			if (logger.isDebugEnabled())
				logger.debug("read NEW_TCP_SOCKET packet  " + p);
			final int forwardInfo = p.extractInt();
			onNewSocket(forwardInfo);
		} else if (p.isProtocol(Packet.HEART_BEAT)) {
			if (logger.isDebugEnabled())
				logger.debug("read heartbeat segment " + p);
			p.setProtocol(Packet.ACK_HEART_BEAT);
			if (logger.isDebugEnabled())
				logger.debug("write ack heartbeat segment " + p);
		}
	}

	private void onNewSocket(final int forwardInfo) throws IOException {
		ClientPipeConfig pConfig = new ClientPipeConfig(this.config);
		pConfig.setForwardInfo(forwardInfo);
		if (rtunnelBindInterfaceName != null) {
			pConfig.setLocalAddress4inbound(NetworkInterfaceHelper
					.getInetAddress(rtunnelBindInterfaceName,
							DEFAULT_IP_VERSION));
		}
		// connect,TODO Time-consuming
		client.newPipe(pConfig);
	}

	@Override
	protected Logger log() {
		return logger;
	}

}
