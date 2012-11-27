package com.cloudbility.rtunnel.common;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.cloudbility.rtunnel.buffer.Packet;

/**
 * 
 * @author atlas
 * @date 2012-10-31
 */
public class ReceivingDataAsPacket extends SimpleChannelUpstreamHandler {
	/**
	 * 
	 * @author atlas
	 * @date 2012-10-31
	 */
	// 64KB
	public static final int bufSize = 1024 * 64;
	
	// TODO check it if thread safe,I think netty ioworker will make sure one
	// thread through this decode at a time.
	private Packet recievedPacket;

	public ReceivingDataAsPacket() {
		recievedPacket = new Packet(bufSize);
	}

	public ReceivingDataAsPacket(int size) {
		recievedPacket = new Packet(size >= 0 ? size : bufSize);
	}

	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof ChannelBuffer) {
			ChannelBuffer buffer = (ChannelBuffer) msg;
			//TODO find why must create a new packet buffer 
			//recievedPacket = new Packet(bufSize);
			recievedPacket.clear();
			recievedPacket.setProtocol(Packet.DATA);
			recievedPacket.readData(buffer);

			Channels.fireMessageReceived(ctx, recievedPacket);
		} else {
			super.messageReceived(ctx, e);
		}
	};
}
