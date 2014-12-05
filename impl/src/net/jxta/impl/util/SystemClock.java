package net.jxta.impl.util;

/**
 * Wrapper interface for roughly millisecond precise system time in java. Primarily
 * exists to allow testing of classes that vary their behaviour based on the current
 * system time.
 * 
 * The default implementation is {@link JavaSystemClock}, which wraps
 * {@link java.lang.System#currentTimeMillis()}.
 */
public interface SystemClock {

	/**
	 * @return the current time, in milliseconds. Results should be equivalent
	 * to those provided by {@link java.lang.System#currentTimeMillis()}.
	 */
	public long getCurrentTime();
	
}
