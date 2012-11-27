package com.cloudbility.rtunnel;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

import com.cloudbility.rtunnel.buffer.Packet;


/**
 * 把一个包的数据写入channel的decoder
 * @author atlas
 * @date 2012-10-30
 */
public class WritePacketDataHandler extends SimpleChannelDownstreamHandler {

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet) {
			Packet cp = (Packet) msg;
			//forward the event to the next handler.
			Channels.write(ctx, e.getFuture(), cp.wrapPacketData());
		} else {
			super.writeRequested(ctx, e);
		}
	}
}
