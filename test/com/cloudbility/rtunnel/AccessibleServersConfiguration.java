/**
 * 
 */
package com.cloudbility.rtunnel;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.common.util.StringUtils;

/**
 * Agent可访问服务器列表配置
 * @author gongdewei
 * @date 2012-8-29
 */
public class AccessibleServersConfiguration {

	private static final String configFile = "conf/accessible_servers.conf";
	private static final Logger log = LoggerFactory.getLogger(AccessibleServersConfiguration.class);
	
	private static List<AccessibleServer> servers = new ArrayList<AccessibleServer>();
	static {
		load();
	}
	
	public static void load() {
		servers.clear();
		File file = new File(configFile);
		log.debug("loading configuration: {}", file.getAbsolutePath());
		try {
			Scanner scanner = new Scanner(file);
			String str = null;
			while(scanner.hasNextLine()) {
				str = scanner.nextLine().trim();
				if(StringUtils.isEmpty(str) || str.startsWith("#")) {
					continue;
				}
				String[] values = new String[4];
				int index = 0;
				Scanner scanner2 = new Scanner(str);
				while(scanner2.hasNext() && index < values.length) {
					values[index++] = scanner2.next();
				}
				if(index >= 3) {
					AccessibleServer server = new AccessibleServer();
					server.setName(values[0]);
					server.setHostname(values[1]);
					server.setIp(values[2]);
					server.setDescription(values[3]);
					servers.add(server);
				}else {
					log.warn("ignore line: {}", str);
				}
			}
			log.info("loaded configuration: \n{}", servers);
		} catch (Exception e) {
			log.error("load configuration failed: "+e.getMessage(), e);
		}
	}
	
	public static List<AccessibleServer> getServers() {
		return servers;
	}
	
	public static List<String> getServerIps(){
		List<String> serverIps = new ArrayList<String>();
		for(AccessibleServer server : servers){
			serverIps.add(server.getIp());
		}
		return serverIps;
	}
	
	public static void save() {
		//TODO
	}
	
	/**
	 * 判断目标地址是否可以访问
	 * @param addr
	 * @return
	 */
	public static boolean isAccessible(InetAddress addr) {
		String ip = addr.getHostAddress();
		for (int i = 0; i < servers.size(); i++) {
			AccessibleServer server = servers.get(i);
			if(StringUtils.equals(ip, server.getIp())) {
				return true;
			}
			//增加其它匹配方式：如网段 192.168.10.0
		}
		return false;
		
	}
}
