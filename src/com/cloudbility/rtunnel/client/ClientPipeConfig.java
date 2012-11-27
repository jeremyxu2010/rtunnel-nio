package com.cloudbility.rtunnel.client;

import java.net.InetAddress;

/**
 * config for a pipe in agentd
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class ClientPipeConfig extends ClientConfig {

	private InetAddress localAddress4Outbound = ControlTunnelHandler.LOCAL_INET_ADDRESS;

	private int localPort4Outbound = 0;

	private InetAddress localAddress4inbound = ControlTunnelHandler.LOCAL_INET_ADDRESS;
	private int localPort4inbound = 0;

	private int forwardPort = -1;

	private int forwardClientId = -1;

	private int forwardInfo;

	public ClientPipeConfig() {
	}

	public ClientPipeConfig(ClientConfig config) {
		super(config);
	}

	public InetAddress getLocalAddress4Outbound() {
		return localAddress4Outbound;
	}

	public void setLocalAddress4Outbound(InetAddress localAddress4Outbound) {
		this.localAddress4Outbound = localAddress4Outbound;
	}

	public int getLocalPort4Outbound() {
		return localPort4Outbound;
	}

	public void setLocalPort4Outbound(int localPort4Outbound) {
		this.localPort4Outbound = localPort4Outbound;
	}

	public InetAddress getLocalAddress4inbound() {
		return localAddress4inbound;
	}

	public void setLocalAddress4inbound(InetAddress localAddress4inbound) {
		this.localAddress4inbound = localAddress4inbound;
	}

	public int getLocalPort4inbound() {
		return localPort4inbound;
	}

	public void setLocalPort4inbound(int localPort4inbound) {
		this.localPort4inbound = localPort4inbound;
	}

	public int getForwardClientId() {
		return forwardClientId;
	}

	public void setForwardInfo(int forwardInfo) {
		this.forwardInfo = forwardInfo;
		this.forwardPort = (int) (forwardInfo >>> 16);
		this.forwardClientId = (int) (forwardInfo & 0xffff);
	}

	public int getForwardInfo() {
		return forwardInfo;
	}

}
