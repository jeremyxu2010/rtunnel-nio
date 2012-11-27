package com.cloudbility.rtunnel;

/**
 * 
 * @author atlas
 * @date 2012-11-1
 */
public class RtunnelConfig {

	/**
	 * rtunnel server host
	 */
	private String rtunnelServerHost;
	/**
	 * rtunnel server port
	 */
	private int rtunnelServerPort;

	public RtunnelConfig() {
	}

	public RtunnelConfig(RtunnelConfig config) {
		this.rtunnelServerHost = config.rtunnelServerHost;
		this.rtunnelServerPort = config.rtunnelServerPort;
	}

	// private String rtunnelServerBindAddress;

	public String getRtunnelServerHost() {
		return rtunnelServerHost;
	}

	public void setRtunnelServerHost(String rtunnelServerHost) {
		this.rtunnelServerHost = rtunnelServerHost;
	}

	public int getRtunnelServerPort() {
		return rtunnelServerPort;
	}

	public void setRtunnelServerPort(int rtunnelServerPort) {
		this.rtunnelServerPort = rtunnelServerPort;
	}

	@Override
	public String toString() {
		return "{rtunnelServerHost=" + rtunnelServerHost
				+ ",rtunnelServerPort=" + rtunnelServerPort + "}";
	}

}
