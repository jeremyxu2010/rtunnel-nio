package com.cloudbility.rtunnel.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 一个ForwardServer是bind在RTunnel Server的某个端口上，并监听需要forward到RTunnel
 * Client的连接，它专属于某个RTunnel Client
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class ForwardServer {
	private static final Logger log = LoggerFactory
			.getLogger(ForwardServer.class);
	/**
	 * bind forwardPort 的server channel
	 */
	private Channel forwardChannel;

	private ForwardConfig config;

	/**
	 * 连接forwardServer的客户端
	 */
	private Map<Integer, ForwardHandler> forwardClients = new ConcurrentHashMap<Integer, ForwardHandler>();

	/**
	 * forwardClientIds的值不会超过本机可以分配端口数目可以用2byte存储
	 * 
	 * (config.getForwardPort(),nextId())是唯一
	 */
	private final AtomicInteger forwardClientIds = new AtomicInteger(0);

	public Channel getForwardChannel() {
		return forwardChannel;
	}

	public void setForwardChannel(Channel server) {
		this.forwardChannel = server;
	}

	public void setConfig(ForwardConfig config) {
		this.config = config;
	}

	public ForwardConfig getConfig() {
		return config;
	}

	public int nextId() {
		return forwardClientIds.incrementAndGet();
	}

	public void addClient(final ForwardHandler handler) {
		forwardClients.put(handler.getForwardClientId(), handler);
		// clean up from forwardClients after closed.
		handler.getChannel().getCloseFuture()
				.addListener(new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future)
							throws Exception {
						forwardClients.remove(handler.getForwardClientId());
					}
				});
	}

	public ForwardHandler getForwardHandler(int bindPort) {
		return forwardClients.get(bindPort);
	}

	/**
	 * close any channel associated with this forward server:forward client
	 * channels,rtunnel server's data channels, this rtunnel client's control
	 * channel
	 */
	public void close() {
		if (log.isDebugEnabled()) {
			log.debug("closing ForwardServer " + this);
		}
		for (ForwardHandler fh : forwardClients.values()) {
			fh.getChannel().close();
		}
		forwardClients.clear();
		forwardChannel.close();
		config.getClientChannel().close();
	}

	@Override
	public String toString() {
		return "ForwardServer{forwardChannel=" + forwardChannel + ",config="
				+ config + "}";
	}
}
