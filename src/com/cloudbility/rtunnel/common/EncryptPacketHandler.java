/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-11-27  Created
*/ 
package com.cloudbility.rtunnel.common; 

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import com.cloudbility.rtunnel.buffer.Packet;

 
public class EncryptPacketHandler extends CommonHandler implements ChannelHandler {

	private CipherHolder cipherHolder;

	public EncryptPacketHandler(CipherHolder cipherHolder) {
	    this.cipherHolder = cipherHolder;
    }

	
	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet && ((Packet)msg).isProtocol(Packet.DATA) && cipherHolder.getCipher() != null) {
			Packet p = (Packet)msg;
			p.setCipher(cipherHolder.getCipher());
			p.setEncrypted();
		}
	    super.writeRequested(ctx, e);
	}
}
