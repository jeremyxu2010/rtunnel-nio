package com.cloudbility.rtunnel;

import com.cloudbility.rtunnel.server.RTunnelServerBootstrap;
import com.cloudbility.rtunnel.server.ServerConfig;

/**
 * 
 * @author atlas
 * @date 2012-10-31
 */
public class AgentSrv {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		ServerConfig config= new ServerConfig();
		config.setForwardBindAddress("localhost");
		config.setRtunnelServerPort(8323);
		startAgenSrv(config);
	}

	private static void startAgenSrv(ServerConfig config) throws Exception {
		RTunnelServerBootstrap server = new RTunnelServerBootstrap();
		server.setConfig(config);
		server.start();
	}
}
