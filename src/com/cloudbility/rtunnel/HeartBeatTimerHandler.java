package com.cloudbility.rtunnel;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.rtunnel.buffer.Packet;

/**
 * 在HeartBeatHandler的前面必须安装读取完整包和写入完整包的handlers，当前的handler会检查lastReadTime/lastWriteTime，如果超过了一个心跳周期则会发送心跳包。
 * 
 * 当前的HeartBeatTimerHandler会过滤掉所有心跳包和ACK心跳包，功能在HeartBeatFilterHandler里面
 * 
 * @author atlas
 * @date 2012-11-1
 */
public class HeartBeatTimerHandler extends HeartBeatFilterHandler implements Runnable {
	public static final int DEFAULT_HEARTBEAT_INTERVAL = 5000;
	public static final int DEFAULT_HEARTBEAT_TIMEOUT = 100000;

	private static Logger logger = LoggerFactory
			.getLogger(HeartBeatTimerHandler.class);

	private static final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(
			2, new NamedThreadFactory("RTunnel-Heart-Beat", true));

	private long lastReadTime = -1;
	private long lastWriteTime = -1;
	/**
	 * <=0 means no heartbeat,ms
	 */
	private int heartbeatInterval = 5000;
	/**
	 * 10 ms
	 */
	private int heartbeatTimeout = 1000000;

	private ScheduledFuture<?> heatbeatTimer;

	private static final Packet packet = new Packet(0);
	static{
		packet.setHeartBeat();
		packet.fillHeader();
	}

	public HeartBeatTimerHandler() {
		this(DEFAULT_HEARTBEAT_INTERVAL, DEFAULT_HEARTBEAT_TIMEOUT);
	}

	public HeartBeatTimerHandler(int heartbeatInterval, int heartbeatTimeout) {
		super();
		this.heartbeatInterval = heartbeatInterval;
		this.heartbeatTimeout = heartbeatTimeout;

	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		startHeatbeatTimer();
		super.channelConnected(ctx, e);
	}

	@Override
	public void run() {
		try {
			long now = System.currentTimeMillis();
			if (getChannel().isOpen()) {
				if ((now - lastReadTime > heartbeatInterval)
						|| (now - lastWriteTime > heartbeatInterval)) {
					if (logger.isTraceEnabled()) {
						logger.debug("write a heartbeat packet to "
								+ getChannel().getRemoteAddress());
					}
					getChannel().write(packet);
				}
				if (now - lastReadTime > heartbeatTimeout) {
					logger.warn("Close channel " + getChannel()
							+ ", because heartbeat read idle time out: "
							+ heartbeatTimeout + "ms");
					getChannel().close();
					stopHeartbeatTimer();
				}
			} else {
				stopHeartbeatTimer();
			}
		} catch (Throwable e) {
			logger.warn("Exception when heartbeat to remote channel "
					+ getChannel(), e);
		}
	}

	@Override
	public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e)
			throws Exception {
		lastWriteTime = System.currentTimeMillis();
		super.writeComplete(ctx, e);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		lastReadTime = System.currentTimeMillis();
		super.messageReceived(ctx, e);
	}

	private void startHeatbeatTimer() {
		stopHeartbeatTimer();
		if (heartbeatInterval > 0) {
			heatbeatTimer = scheduled
					.scheduleWithFixedDelay(this, heartbeatInterval,
							heartbeatInterval, TimeUnit.MILLISECONDS);
		}
	}

	private void stopHeartbeatTimer() {
		if (heatbeatTimer != null && !heatbeatTimer.isCancelled()) {
			try {
				heatbeatTimer.cancel(true);
				scheduled.purge();
			} catch (Throwable e) {
				if (logger.isWarnEnabled()) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
		heatbeatTimer = null;
	}

	@Override
	protected Logger log() {
		return logger;
	}

}
