/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-11-27  Created
*/ 
package com.cloudbility.rtunnel.server; 

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import com.cloudbility.rtunnel.buffer.Packet;
import com.cloudbility.rtunnel.common.CommonHandler;
 
public class RetrieveCompressFlagHandler extends CommonHandler {
	private AtomicBoolean compressed;

	public RetrieveCompressFlagHandler(AtomicBoolean compressed) {
	    this.compressed = compressed;
    }

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet && ((Packet)msg).isProtocol(Packet.DATA)) {
			Packet p = (Packet) msg;
			compressed.set(p.isCompressed());
		}
		super.messageReceived(ctx, e);
	}
}
