package net.jxta.impl.util.threads;

import java.util.concurrent.Callable;

/**
 * Simple wrapper for a Runnable to make it conform to the Callable interface.
 */
public class RunnableAsCallableWrapper<T> implements Callable<T> {

	private Runnable wrapped;
	
	public RunnableAsCallableWrapper(Runnable wrapped) {
		this.wrapped = wrapped;
	}
	
	public T call() throws Exception {
		wrapped.run();
		return null;
	}
}
