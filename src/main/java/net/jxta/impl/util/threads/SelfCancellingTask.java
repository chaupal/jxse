package net.jxta.impl.util.threads;

import java.util.concurrent.ScheduledFuture;

/**
 * Runnable which can be provided with it's own ScheduledFuture handle to cancel
 * itself. This is intended to be equivalent to TimerTask, working around the
 * inability for Runnables or Callables to cancel themselves in a similar fashion.
 */
public abstract class SelfCancellingTask implements Runnable {

	private boolean cancelled;
	private ScheduledFuture<?> handle;
	private int runCount = 0;
	
	/**
	 * Checks whether the task is already cancelled, and if not invokes
	 * {@link #execute()}.
	 */
	public final void run() {
		if(cancelled) {
			cancelViaHandle();
			return;
		}
		
		runCount++;
		execute();
	}
	
	/**
	 * Analogous to {@link java.util.TimerTask#run()}.
	 */
	protected abstract void execute();

	public void cancel() {
		this.cancelled = true;
		cancelViaHandle();
	}
	
	private void cancelViaHandle() {
		if(handle != null) {
			handle.cancel(false);
		}
	}
	
	public void setHandle(ScheduledFuture<?> handle) {
		this.handle = handle;
	}
	
	public int getRunCount() {
		return runCount;
	}

}
