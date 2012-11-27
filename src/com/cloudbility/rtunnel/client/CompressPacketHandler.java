/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-11-27  Created
*/ 
package com.cloudbility.rtunnel.client; 

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import com.cloudbility.rtunnel.buffer.Packet;
import com.cloudbility.rtunnel.common.CommonHandler;
import com.skybility.cloudsoft.agent.common.AdvancedProperties;
 
public class CompressPacketHandler extends CommonHandler {

	private TunnelConfig tunnelConfig;
	
	// if package's length less than this,do not compress.
	private int compressThreshold = AdvancedProperties.getInstance().requireInteger("compressThreshold");

	public CompressPacketHandler() {
		
    }
	
	public CompressPacketHandler(TunnelConfig tunnelConfig) {
	    this.tunnelConfig = tunnelConfig;
    }
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet && ((Packet)msg).isProtocol(Packet.DATA)) {
			Packet cp = (Packet) msg;
			cp.decode();
		}
		super.messageReceived(ctx, e);
	}
	
	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (this.tunnelConfig != null && msg instanceof Packet && ((Packet)msg).isProtocol(Packet.DATA)) {
			Packet p = (Packet) msg;
			boolean isCompressed = this.tunnelConfig.isCompressed();
			if (isCompressed) {
				int uncompressed = p.getDataLen();
				if (uncompressed > compressThreshold) {
					p.setCompressed();
				}
				p.encode();
				if (uncompressed > compressThreshold) {
					int compressed = p.getDataLen();
					this.tunnelConfig.setCompressRatio(compressed, uncompressed);
				}
			}
		}
	    super.writeRequested(ctx, e);
	}
	
}
