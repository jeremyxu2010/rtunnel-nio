/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-11-27  Created
*/ 
package com.cloudbility.rtunnel.common; 

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import com.cloudbility.rtunnel.buffer.Packet;
import com.cloudbility.rtunnel.common.CommonHandler;
 
public class DataPacketProcessorHandler extends CommonHandler {
	private CipherHolder cipherHolder;

	public DataPacketProcessorHandler(CipherHolder cipherHolder) {
	    this.cipherHolder = cipherHolder;
    }

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet && ((Packet)msg).isProtocol(Packet.DATA)) {
			Packet cp = (Packet) msg;
			if(cipherHolder.getCipher() != null){
				cp.setCipher(cipherHolder.getCipher());
			}
			cp.decode();
		}
		super.messageReceived(ctx, e);
	}
	
	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet && ((Packet)msg).isProtocol(Packet.DATA)) {
			Packet p = (Packet) msg;
			p.encode();
		}
	    super.writeRequested(ctx, e);
	}
}
