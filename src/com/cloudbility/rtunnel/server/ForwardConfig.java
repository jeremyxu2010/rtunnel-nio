package com.cloudbility.rtunnel.server;

import org.jboss.netty.channel.Channel;

/**
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class ForwardConfig {
	private int forwardPort;
	/**
	 * client control channel
	 */
	private Channel clientChannel;

	public void setClientChannel(Channel clientChannel) {
		this.clientChannel = clientChannel;
	}

	public void setForwardPort(int forwardBindPort) {
		this.forwardPort = forwardBindPort;
	}

	public Channel getClientChannel() {
		return clientChannel;
	}

	public int getForwardPort() {
		return forwardPort;
	}

	@Override
	public String toString() {
		return "ForwardConfig{forwardPort=" + forwardPort + ",control channel="
				+ clientChannel + "}";
	}
}
