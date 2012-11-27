package com.cloudbility.rtunnel;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import socks.ProxyServer;
import socks.server.ServerAuthenticator;
import socks.server.ServerAuthenticatorNone;

import com.cloudbility.rtunnel.client.ClientConfig;
import com.cloudbility.rtunnel.client.RTunnelClientBootstrap;

/**
 * 
 * @author atlas
 * @date 2012-10-31
 */
public class AgentD {
	private static final Logger logger = LoggerFactory.getLogger(AgentD.class);

	public static void main(String[] args) throws Exception {
		startProxyServer("localhost", 1080);
		ClientConfig config = new ClientConfig();
		config.setRtunnelServerPort(8323);
		config.setRtunnelServerHost("localhost");
		config.setForwardPort(8325);
		config.setProxyServerHost("localhost");
		config.setProxyServerPort(1080);
		startAgentD(config);
	}

	static void startAgentD(ClientConfig config) throws Exception{
		RTunnelClientBootstrap client = new RTunnelClientBootstrap();
		client.setConfig(config);
//		client.start();
		ImmortalBootstrap ibs = new ImmortalBootstrap();
		ibs.setBootstrap(client);
		ibs.start();
	}
	static void startProxyServer(final String host, final int port) {
		Runnable socksServerRunnable = new Runnable() {
			public void run() {
				ServerAuthenticator auth = new ServerAuthenticatorNone();
				ProxyServer socksServer = new ProxyServer(auth);
				String socks5_bindAddress = host;
				int socks5_port = port;
				int socks5_backlog = 20;
				InetAddress socks5BindAddress = null;
				try {
					socks5BindAddress = InetAddress
							.getByName(socks5_bindAddress);
				} catch (UnknownHostException e) {
					logger.error("get local host address error.");
					System.exit(1);
				}
				ProxyServer.setLog(System.out);
				socksServer.start(socks5_port, socks5_backlog,
						socks5BindAddress);
			}
		};
		Thread socksServerThread = new Thread(socksServerRunnable);
		socksServerThread.start();
	}
}
