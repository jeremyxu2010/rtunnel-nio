package com.cloudbility.rtunnel.common;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import com.cloudbility.rtunnel.buffer.Packet;

/**
 * 读一个完整的数据包
 * 
 * @author atlas
 * @date 2012-10-29
 */
public class ReceivingPacketHandler extends FrameDecoder {

	public static final int HEADER_SIZE = 5;

	// 64KB
	public static final int bufSize = 1024 * 64;

	//TODO check it if thread safe,I think netty ioworker will make sure one thread through this decode at a time.
	private Packet recievedPacket;
	
	private int size;

	public ReceivingPacketHandler() {
		recievedPacket = new Packet(bufSize);
	}

	public ReceivingPacketHandler(int size) {
		this.size=size;
		recievedPacket = new Packet(size >= 0 ? size : bufSize);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buf) throws Exception {
		if (buf.readableBytes() < HEADER_SIZE) {
			// The header length field was not received yet - return null.
			return null;
		}
		buf.markReaderIndex();
		// Read protocol header
		buf.readByte();
		// Read the data length field.
		int length = buf.readInt();
		buf.resetReaderIndex();
		// Make sure if there's enough bytes in the buffer.
		int totalLength = length + HEADER_SIZE;
		if (buf.readableBytes() < totalLength) {
			return null;
		}
		//TODO 也许可以共享这个对象
		//recievedPacket = new Packet(size >= 0 ? size : bufSize);
		recievedPacket.clear();
		recievedPacket.readPacket(buf, totalLength);
		return recievedPacket;
	}
}
