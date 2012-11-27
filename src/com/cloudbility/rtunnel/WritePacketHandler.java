package com.cloudbility.rtunnel;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

import com.cloudbility.rtunnel.buffer.Packet;

/**
 * 把一个完整的包写入channel的Decoder
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class WritePacketHandler extends SimpleChannelDownstreamHandler {

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet) {
			Packet cp = (Packet) msg;
			//forward the event to the next handler.
			Channels.write(ctx,e.getFuture(),cp.wrapPacket());
		} else {
			super.writeRequested(ctx, e);
		}
	}
}
