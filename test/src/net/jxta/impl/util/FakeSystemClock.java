
package net.jxta.impl.util;

/**
 * SystemClock implementation that allows tests to directly set what the current time is.
 * Useful to precisely controlling behaviour of time-dependent functionality.
 */
public class FakeSystemClock implements SystemClock {

	public long currentTime = 0L;

	public long getCurrentTime() {
		return currentTime;
	}

}
