package com.cloudbility.rtunnel;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.cloudbility.rtunnel.buffer.Packet;


/**
 * 把一个包解开，只接收数据
 * 
 * @author atlas
 * @date 2012-10-29
 */
public class ReceivingPacketDataHandler extends SimpleChannelUpstreamHandler {
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet) {
			Packet cp = (Packet) msg;
			ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(cp.getBuffer(),
					Packet.HEAD_SIZE, cp.getIndex());
			// forward to next handler
			Channels.fireMessageReceived(ctx, buffer);
		} else {
			super.messageReceived(ctx, e);
		}
	}
}
