package com.cloudbility.rtunnel.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.RTunnelBootstrap;
import com.cloudbility.rtunnel.buffer.Packet;

/**
 * restart supported,but not test.
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class RTunnelClientBootstrap extends RTunnelBootstrap {
	private static final Logger log = LoggerFactory
			.getLogger(RTunnelClientBootstrap.class);
	/**
	 * rtunnel client config
	 */
	private ClientConfig config;

	/**
	 * control channel from rtunnel client to rtunnel server.
	 */
	private Channel controlChannel;
	private ClientSocketChannelFactory cscf;

	private ConcurrentMap<Integer, ClientOutboundInfo> handlers = new ConcurrentHashMap<Integer, ClientOutboundInfo>();

	protected void doStart() throws Exception {
		if (config == null) {
			throw new NullPointerException("config is null");
		}
		log.info("starting rtunnel client " + config);
		// establish a control channel from client to server.
		// Configure the client.
		cscf = new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());
		ClientBootstrap bootstrap = new ClientBootstrap(cscf);
		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ClientControlChannelPipelineFactory(
				this, config));

		ChannelFuture future = bootstrap.connect(new InetSocketAddress(config
				.getRtunnelServerHost(), config.getRtunnelServerPort()));

		// wait for connect to complete
		future.awaitUninterruptibly();

		if (future.getCause() != null) {
			cleanupSocketChannelFactory();
			throw new IOException("Error when connecting agentSrv:"
					+ config.getRtunnelServerHost() + ":"
					+ config.getRtunnelServerPort(), future.getCause());
		}
		controlChannel = future.getChannel();
		// if server close it,clean up
		// TODO check race condition
		controlChannel.getCloseFuture().addListener(
				new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future)
							throws Exception {
						stop();
					}
				});
	}

	public void newPipe(final ClientPipeConfig config) {
		ClientOutboundInfo info = new ClientOutboundInfo();
		info.setConfig(config);
		ClientOutboundInfo preInfo = handlers.put(config.getForwardClientId(),
				info);
		if (preInfo != null) {
			// may never happend
			preInfo.getOutboundChannel().close();
		}
		// connect to proxy server first
		ClientBootstrap bootstrap = new ClientBootstrap(cscf);
		bootstrap.setPipelineFactory(new ClientOutboundChannelPipelineFactory(
				config));
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(config
				.getProxyServerHost(), config.getProxyServerPort()));

		final Channel outboundChannel = future.getChannel();
		ClientOutboundHandler outboundHandler = (ClientOutboundHandler) outboundChannel
				.getPipeline().getLast();
		info.setOutboundChannel(outboundChannel);
		info.setOutboundHandler(outboundHandler);

		// then connect to rtunnel server and request for a data channel.
		bootstrap = new ClientBootstrap(cscf);
		bootstrap.setPipelineFactory(new ClientDataChannelPipelineFactory(info,
				config));
		future = bootstrap.connect(new InetSocketAddress(this.config
				.getRtunnelServerHost(), this.config.getRtunnelServerPort()));
		Channel inboundChannel = future.getChannel();
		outboundHandler.setInboundChannel(inboundChannel);
	}

	private void cleanupSocketChannelFactory() {
		ClientSocketChannelFactory factory = this.cscf;
		if (factory != null) {
			this.cscf = null;
			factory.releaseExternalResources();
		}
	}

	private void cleanupRtunnelServerChannel() {
		Channel channle = this.controlChannel;
		if (channle != null) {
			this.controlChannel = null;
			try {
				if (channle.isWritable()) {
					Packet p = new Packet(0);
					p = Packet.fillCloseTunnelPacket(p);
					ChannelFuture future = channle.write(p.wrapPacket());
					// wait for close tunnel packet to be sent to rtunnel
					// server.
					try {
						future.await(config.getCloseTunnelWaitTimeout());
					} catch (InterruptedException e) {
						// ignore
					}

				}
			} finally {
				channle.close();
			}

		}
	}

	private void cleanupOutboundChannel() {
		for (ClientOutboundInfo info : handlers.values()) {
			info.getOutboundChannel().close();
		}
		handlers.clear();
	}

	@Override
	protected Runnable getStopJob() {
		return new Runnable() {
			public void run() {
				log.info("stopping rtunnel client " + config);
				cleanupRtunnelServerChannel();
				cleanupOutboundChannel();
				cleanupSocketChannelFactory();
			}
		};
	}

	public void setConfig(ClientConfig config) {
		this.config = config;
	}

}
