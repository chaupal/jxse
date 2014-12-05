package net.jxta.impl.util;

/**
 * Default implementation of {@link SystemClock}, which wraps
 * {@link java.lang.System#currentTimeMillis()}.
 */
public class JavaSystemClock implements SystemClock {

	public long getCurrentTime() {
		return System.currentTimeMillis();
	}

}
