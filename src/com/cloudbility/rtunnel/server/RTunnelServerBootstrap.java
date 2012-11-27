package com.cloudbility.rtunnel.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.common.RTunnelBootstrap;

/**
 * restart is supported,but not tested.
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class RTunnelServerBootstrap extends RTunnelBootstrap {
	private static final Logger log = LoggerFactory
			.getLogger(RTunnelServerBootstrap.class);

	private ServerConfig config;

	private Channel serverChannel;

	private NioServerSocketChannelFactory cscf;

	private ConcurrentMap<Integer, ForwardServer> forwardServers = new ConcurrentHashMap<Integer, ForwardServer>();

	@Override
	protected void doStart() throws Exception {
		// Configure the client.
		cscf = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool());
		ServerBootstrap bootstrap = new ServerBootstrap(cscf);

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ServerInboundChannelPipelineFactory(
				this));
		serverChannel = bootstrap.bind(new InetSocketAddress(config
				.getRtunnelServerPort()));
		if (log.isDebugEnabled()) {
			log.debug("Rtunnel server listening on {}",
					config.getRtunnelServerPort());
		}
	}

	/**
	 * 根据config建立一个ForwardServer，监听特定的端口,
	 * 
	 * @param config
	 * @return true 如果建立成功；false不成功，此时应该关闭发送请求的channel
	 */
	public boolean newForwardServer(final ForwardConfig config) {
		ForwardServer server = new ForwardServer();
		server.setConfig(config);
		// for race condition
		ForwardServer preServer = forwardServers.putIfAbsent(
				config.getForwardPort(), server);
		if (preServer == null) {
			ServerBootstrap bootstrap = new ServerBootstrap(cscf);
			bootstrap
					.setPipelineFactory(new ForwardServerChannelPipelineFactory(
							this, server));
			final Channel forwardServerChannel = bootstrap
					.bind(new InetSocketAddress(config.getForwardPort()));
			server.setForwardChannel(forwardServerChannel);
			// if control channel is closed,close ForwardServer
			config.getClientChannel().getCloseFuture()
					.addListener(new ChannelFutureListener() {
						public void operationComplete(ChannelFuture future)
								throws Exception {
							forwardServerChannel.close();
						}
					});
			// clean up all forward client's channel after closed
			forwardServerChannel.getCloseFuture().addListener(
					new ChannelFutureListener() {
						public void operationComplete(ChannelFuture future)
								throws Exception {
							onControlTunnelClosed(config.getForwardPort());
						}
					});
			return true;
		} else {
			log.warn(
					"agent client {} request to establish a forward server binding port {},this port is Already bound,closing client channel.",
					config.getClientChannel(), config.getForwardPort());
			return false;
		}
	}

	// 将监听在forwardServerPort上的accept到连接channel的remotePort为forwardRemotePort的通道与agentd的数据通道channel建立pipe
	public void newPipe(int forwardServerPort, int forwardClientId,
			ServerTunnelHandler sthandler) throws IOException {
		ForwardServer server = forwardServers.get(forwardServerPort);
		if (server == null) {// forward server is closed
			throw new IOException("Forward server on port " + forwardServerPort
					+ " is closed.");
		}
		ForwardHandler handler = server.getForwardHandler(forwardClientId);
		if (handler == null) {
			throw new IOException("Forward client channel with id "
					+ forwardClientId
					+ " which connected to forward server with port "
					+ forwardServerPort + " is closed.");
		}
		handler.setDataChannel(sthandler.getChannel());
		sthandler.setForwardChannel(handler.getChannel());
	}

	private void cleanup() {
		if (cscf != null) {
			cscf.releaseExternalResources();
			cscf = null;
		}
	}

	@Override
	protected Runnable getStopJob() {
		return new Runnable() {
			public void run() {
				log.info("stopping rtunnel server " + config);
				if (serverChannel != null) {
					serverChannel.close();
					serverChannel = null;
				}
				for (ForwardServer server : forwardServers.values()) {
					server.close();
				}
				forwardServers.clear();
				// finally
				cleanup();
			}
		};
	}

	public void onControlTunnelClosed(int forwardPort) {
		ForwardServer server = forwardServers.remove(forwardPort);
		if (server != null) {
			server.close();
		}
	}

	public void addForwarClient(int forwardBindPort, ForwardHandler handler) {
		ForwardServer server = forwardServers.get(forwardBindPort);
		server.addClient(handler);
	}

	public void setConfig(ServerConfig config) {
		this.config = config;
	}

}
