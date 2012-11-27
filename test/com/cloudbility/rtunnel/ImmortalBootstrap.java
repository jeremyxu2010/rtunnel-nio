package com.cloudbility.rtunnel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbility.common.AbstractLifeCycle;
import com.cloudbility.rtunnel.common.RTunnelBootstrap;

/**
 * 把RTunnelBootstrap包装成一个不死的service，定时检测RTunnelBootstrap的状态，如果处于不正常状态，则启动它
 * 
 * @author atlas
 * @date 2012-11-1
 */
public class ImmortalBootstrap extends AbstractLifeCycle implements Runnable {
	private static final Logger log = LoggerFactory
			.getLogger(ImmortalBootstrap.class);

	private RTunnelBootstrap bootstrap;
	private volatile boolean running = false;
	private Thread restartThread;

	// default check interval 10 s
	private int retryInterval = 10000;

	@Override
	protected void doStart() throws Exception {
		if (bootstrap == null) {
			throw new NullPointerException("bootstrap is null");
		}
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					bootstrap.stop();
				} catch (Exception e) {
				}
			}
		}, "RTunnel-ShutdownHook Thread"));

		running = true;
		restartThread = new Thread(this, "Immortal RTunnel Thread");
		restartThread.setDaemon(true);
		restartThread.start();
	}

	@Override
	public void run() {
		while (running) {
			if (bootstrap.isFailed() || bootstrap.isStopped()) {
				try {
					log.warn("starting RTunnel bootstrap...");
					start(bootstrap);
				} catch (Throwable e) {
					log.error("error when start RTunnel bootstrap,retry after "
							+ retryInterval + " ms", e);
					try {
						stop(bootstrap);
					} catch (Exception e1) {
					}
				}
			}
			try {
				Thread.sleep(retryInterval > 5000 ? retryInterval : 5000);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	protected void doStop() throws Exception {
		try {
			running = false;
			stop(bootstrap);
		} finally {
			if (restartThread != null) {
				restartThread.interrupt();
			}
		}
	}

	public void setBootstrap(RTunnelBootstrap bootstrap) {
		this.bootstrap = bootstrap;
	}

	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}
}
