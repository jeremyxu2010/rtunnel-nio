package com.cloudbility.rtunnel.client;

import org.jboss.netty.channel.Channel;

/**
 * 
 * @author atlas
 * @date 2012-11-1
 */
public class ClientOutboundInfo {
	private Channel outboundChannel;
	
	private ClientOutboundHandler outboundHandler;
	
	private ClientPipeConfig config;

	public Channel getOutboundChannel() {
		return outboundChannel;
	}

	public void setOutboundChannel(Channel outboundChannel) {
		this.outboundChannel = outboundChannel;
	}

	public ClientOutboundHandler getOutboundHandler() {
		return outboundHandler;
	}

	public void setOutboundHandler(ClientOutboundHandler outboundHandler) {
		this.outboundHandler = outboundHandler;
	}

	public ClientPipeConfig getConfig() {
		return config;
	}

	public void setConfig(ClientPipeConfig config) {
		this.config = config;
	}
	
	
	
	
	
}
