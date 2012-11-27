package com.cloudbility.rtunnel.common;

import java.net.InetSocketAddress;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.LifeCycleAwareChannelHandler;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author atlas
 * @date 2012-10-30
 */
public class CommonHandler extends SimpleChannelHandler implements
		LifeCycleAwareChannelHandler {
	private static Logger logger = LoggerFactory.getLogger(CommonHandler.class);

	protected ChannelHandlerContext ctx;

	protected InetSocketAddress localAddress;

	protected InetSocketAddress remoteAddress;

	private String nameSuffix;

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		log().error("Exception catched.", e.getCause());
		e.getChannel().close();
	}

	@Override
	public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		localAddress = (InetSocketAddress) e.getChannel().getLocalAddress();
		remoteAddress = (InetSocketAddress) e.getChannel().getRemoteAddress();
		nameSuffix = "{" + localAddress + "<==>" + remoteAddress + "}";
		super.channelBound(ctx, e);
	}

	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
			throws Exception {
		if (false) {
			String oname = null;
			try {
				oname = Thread.currentThread().getName();
				Thread.currentThread().setName(oname + nameSuffix);
				super.handleDownstream(ctx, e);
			} finally {
				Thread.currentThread().setName(oname);
			}
		} else {
			super.handleDownstream(ctx, e);
		}
	}

	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
			throws Exception {
		if (false) {
			String oname = null;
			try {
				oname = Thread.currentThread().getName();
				Thread.currentThread().setName(
						oname + "{" + localAddress + "<==>" + remoteAddress
								+ "}");
				super.handleUpstream(ctx, e);
			} finally {
				Thread.currentThread().setName(oname);
			}
		} else {
			super.handleUpstream(ctx, e);
		}
	}

	protected Logger log() {
		return logger;
	}

	public Channel getChannel() {
		return ctx.getChannel();
	}

	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	/**
	 * 设置当前通道与通道peerChannel在一方关闭时关闭另一方，通常用于pipe功能的handler
	 * 
	 * @param peerChannel
	 */
	protected void setupMutualCloseListener(final Channel peerChannel) {
		// 相互清理
		peerChannel.getCloseFuture().addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future)
					throws Exception {
				CommonHandler.this.getChannel().close();
			}
		});
		this.getChannel().getCloseFuture()
				.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future)
							throws Exception {
						peerChannel.close();
					}
				});

	}

	@Override
	public void beforeAdd(ChannelHandlerContext ctx) throws Exception {

	}

	@Override
	public void afterAdd(ChannelHandlerContext ctx) throws Exception {
		this.ctx = ctx;
	}

	@Override
	public void beforeRemove(ChannelHandlerContext ctx) throws Exception {

	}

	@Override
	public void afterRemove(ChannelHandlerContext ctx) throws Exception {
		this.ctx = null;
	}
}
