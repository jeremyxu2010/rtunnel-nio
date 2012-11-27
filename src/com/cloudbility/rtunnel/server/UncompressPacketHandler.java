/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-11-27  Created
*/ 
package com.cloudbility.rtunnel.server; 

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

import com.cloudbility.rtunnel.buffer.Packet;
 
public class UncompressPacketHandler extends SimpleChannelDownstreamHandler {
	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet) {
			Packet cp = (Packet) msg;
			cp.decode();
		}
		super.writeRequested(ctx, e);
	}
}
