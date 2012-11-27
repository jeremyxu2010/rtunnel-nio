package com.cloudbility.rtunnel.common;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.buffer.Packet;

/**
 * 经过这个handler以后，接收到的数据如果是心跳或者ACK心跳，则会处理掉，不会继续到下一个handler。
 * 如果是心跳包，则写回去，如果是ack心跳包，则不继续处理
 * @author atlas
 * @date 2012-11-2
 */
public class HeartBeatFilterHandler extends CommonHandler {

	private static Logger logger = LoggerFactory
			.getLogger(HeartBeatFilterHandler.class);

	public HeartBeatFilterHandler() {
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof Packet) {
			Packet packet = (Packet) msg;
			if (packet.isHeartBeat()) {
				if (logger.isTraceEnabled()) {
					logger.trace("received a heart beat packet");
				}
				packet.setAckHeartBeart();
				// write an ack packet back,don't need to use
				// e.getChannel().write(packet);
				Channels.write(ctx, e.getFuture(), packet);
				return;
			} else if (packet.isAckHeartBeat()) {
				if (logger.isTraceEnabled()) {
					logger.trace("received a ack heart beat packet");
				}
				return;
			}
		}
		super.messageReceived(ctx, e);
	}

	@Override
	protected Logger log() {
		return logger;
	}

}
