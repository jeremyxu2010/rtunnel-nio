package com.cloudbility.rtunnel.server;

import com.cloudbility.rtunnel.RtunnelConfig;

/**
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class ServerConfig extends RtunnelConfig {

	private String forwardBindAddress;

	public ServerConfig() {
	}

	public ServerConfig(ServerConfig config) {
		super(config);
		this.forwardBindAddress = config.forwardBindAddress;
	}

	public String getForwardBindAddress() {
		return forwardBindAddress;
	}

	public void setForwardBindAddress(String forwardBindAddress) {
		this.forwardBindAddress = forwardBindAddress;
	}

	@Override
	public String toString() {
		return "{forwardBindAddress=" + forwardBindAddress + ",rtunnelConfig="
				+ super.toString() + "}";
	}
}
