package com.cloudbility.rtunnel;

import java.util.ArrayList;
import java.util.List;

import nu.najt.kecon.jsocksproxy.JSocksProxy;
import nu.najt.kecon.jsocksproxy.configuration.Configuration;
import nu.najt.kecon.jsocksproxy.configuration.Listen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.client.ClientConfig;
import com.cloudbility.rtunnel.client.RTunnelClientBootstrap;
import com.skybility.cloudsoft.agent.common.AdvancedProperties;

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
//				String socks5_bindAddress = host;
//				int socks5_port = port;
//				int socks5_backlog = 20;
//				InetAddress socks5BindAddress = null;
//				try {
//					socks5BindAddress = InetAddress
//							.getByName(socks5_bindAddress);
//				} catch (UnknownHostException e) {
//					logger.error("get local host address error.");
//					System.exit(1);
//				}
//				
//				ServerAuthenticator auth = new ServerAuthenticatorNone();
//				ProxyServer socksServer = new ProxyServer(auth);
//				
//				ProxyServer.setLog(System.out);
//				socksServer.start(socks5_port, socks5_backlog,
//						socks5BindAddress);
				
				String socks5_bindAddress = host;
				int socks5_port = port;
				int socks5_backlog = 20;
				
				JSocksProxy socksServer = JSocksProxy.singleton;
				Configuration config = new Configuration();
				config.setAllowSocks4(true);
				config.setAllowSocks5(true);
				config.setBacklog(socks5_backlog);
				Listen l = new Listen();
				l.setAddress(socks5_bindAddress);
				l.setPort(socks5_port);
				List<Listen> listens = new ArrayList<Listen>();
				listens.add(l);
				config.setListen(listens);
				List<String> outgoingAddresses = new ArrayList<String>();
				//根据是否检查访问地址的配置使用不同的验证器
				boolean checkAccess = AdvancedProperties.getInstance().getAsBoolean("socksServer_checkAccess");
				if(checkAccess) {
					outgoingAddresses.addAll(AccessibleServersConfiguration.getServerIps());
				}else {
					outgoingAddresses.add("0.0.0.0");
				}
				config.setOutgoingAddresses(outgoingAddresses);
				config.setTerminateTimeout(AdvancedProperties.getInstance()
						.requireLong("esShutdownMaxTime"));
				socksServer.setConfiguration(config);
				socksServer.start();
				
				
				
			}
		};
		Thread socksServerThread = new Thread(socksServerRunnable);
		socksServerThread.start();
	}
}
