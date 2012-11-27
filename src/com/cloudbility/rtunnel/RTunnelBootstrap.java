package com.cloudbility.rtunnel;

import org.jboss.netty.util.internal.DeadLockProofWorker;

import com.cloudbility.common.AbstractLifeCycle;

/**
 * 
 * @author atlas
 * @date 2012-11-1
 */
public abstract class RTunnelBootstrap extends AbstractLifeCycle {

	/**
	 * 调用者的线程是否是netty的io线程
	 * 
	 * @return
	 */
	protected boolean isCurrentIOThread() {
		return DeadLockProofWorker.PARENT.get() != null;
	}

	protected void doStop() throws Exception {
		Runnable stopJob = getStopJob();
		if (stopJob == null) {
			throw new NullPointerException("stopJob is null");
		}
		if (isCurrentIOThread()) {// in io thread
			Thread stopThread = new Thread(stopJob, "Cleanup Thread");
			stopThread.start();
		} else {
			stopJob.run();
		}
	}

	protected abstract Runnable getStopJob();

}
