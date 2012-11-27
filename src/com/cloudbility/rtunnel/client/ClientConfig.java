package com.cloudbility.rtunnel.client;

import com.cloudbility.rtunnel.common.RtunnelConfig;

/**
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class ClientConfig extends RtunnelConfig {

	/**
	 * forward server port listening on agentSrv
	 */
	private int forwardPort;

	/**
	 * proxy server host
	 */
	private String proxyServerHost;

	/**
	 * proxy server port
	 */
	private int proxyServerPort;

	/**
	 * timeout in ms for close tunnel packet to be sent. when timeout
	 * expired,close the tunnel anyway.
	 */
	private int closeTunnelWaitTimeout = 2000;

	public ClientConfig() {
	}

	public ClientConfig(ClientConfig config) {
		super(config);
		this.forwardPort = config.forwardPort;
		this.proxyServerHost = config.proxyServerHost;
		this.proxyServerPort = config.proxyServerPort;
	}

	public int getForwardPort() {
		return forwardPort;
	}

	public void setForwardPort(int forwardPort) {
		this.forwardPort = forwardPort;
	}

	public String getProxyServerHost() {
		return proxyServerHost;
	}

	public void setProxyServerHost(String proxyServerHost) {
		this.proxyServerHost = proxyServerHost;
	}

	public int getProxyServerPort() {
		return proxyServerPort;
	}

	public void setProxyServerPort(int proxyServerPort) {
		this.proxyServerPort = proxyServerPort;
	}

	public int getCloseTunnelWaitTimeout() {
		return closeTunnelWaitTimeout;
	}

	public void setCloseTunnelWaitTimeout(int closeTunnelWaitTimeout) {
		this.closeTunnelWaitTimeout = closeTunnelWaitTimeout;
	}

	@Override
	public String toString() {
		return "{forwardPort=" + forwardPort + ",proxyServerHost="
				+ proxyServerHost + ",proxyServerPort=" + proxyServerPort
				+ ",RtunnelServer=" + super.toString() + "}";
	}
}
